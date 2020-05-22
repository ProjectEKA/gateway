package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.link.common.Validator;
import in.projecteka.gateway.link.common.model.ErrorResult;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DiscoveryValidatorTest {

    @Mock
    HttpEntity<String> requestEntity;
    @Mock
    HttpHeaders httpHeaders;
    Validator discoveryValidator;
    @Captor
    ArgumentCaptor<Error> errorArgumentCaptor;
    @Captor
    ArgumentCaptor<ErrorResult> discoveryResultArgumentCaptor;
    @Mock BridgeRegistry bridgeRegistry;
    @Mock
    CMRegistry cmRegistry;
    @Mock
    DiscoveryServiceClient discoveryServiceClient;
    @Mock
    CacheAdapter<String,String> requestIdMappings;
    @Mock
    YamlRegistryMapping cmConfig;
    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        discoveryValidator = Mockito.spy(new Validator(bridgeRegistry,cmRegistry,requestIdMappings));
    }

    @Test
    public void shouldThrowErrorWhenHIPHeaderIsNotPresent() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get("X-HIP-ID")).thenReturn(Collections.emptyList());

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity))
                .verifyErrorSatisfies(throwable -> assertThat(throwable)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class)).isEqualToComparingFieldByField(ClientError.hipIdMissing()));
    }

    @Test
    public void shouldThrowErrorWhenNoMappingIsFoundForHIPId() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testHipId = "testHipId";
        when(httpHeaders.get("X-HIP-ID")).thenReturn(Arrays.asList(testHipId));
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.empty());

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity))
                .verifyErrorSatisfies(throwable -> assertThat(throwable)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class)).isEqualToComparingFieldByField(ClientError.mappingNotFoundForHipId()));

    }

    @Test
    public void shouldReturnEmptyWhenNoRequestIdIsFound() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        Map<String,Object> requestBody = new HashMap<>();
        when(requestEntity.getBody()).thenReturn(new ObjectMapper().writeValueAsString(requestBody));
        String testHipId = "testHipId";
        when(httpHeaders.get("X-HIP-ID")).thenReturn(Arrays.asList(testHipId));
        YamlRegistryMapping hipConfig = new YamlRegistryMapping();
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldReturnValidatedRequest() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        Map<String,Object> requestBody = new HashMap<>();
        String cmRequestId = "testRequestId";
        requestBody.put("requestId", cmRequestId);
        when(requestEntity.getBody()).thenReturn(new ObjectMapper().writeValueAsString(requestBody));
        String testHipId = "testHipId";
        when(httpHeaders.get("X-HIP-ID")).thenReturn(Arrays.asList(testHipId));
        YamlRegistryMapping hipConfig = new YamlRegistryMapping();
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity))
                .assertNext(validatedDiscoverRequest -> {
                    assertThat(hipConfig).isEqualTo(validatedDiscoverRequest.getHipConfig());
                    assertThat(requestBody).isEqualTo(validatedDiscoverRequest.getDeserializedRequest());
                    assertThat(cmRequestId).isEqualTo(validatedDiscoverRequest.getCmRequestId());
                }) .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenCMHeaderIsNotPresent() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get("X-CM-ID")).thenReturn(Collections.emptyList());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenNoMappingIsFoundForCMId() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testCmId = "testCMId";
        when(httpHeaders.get("X-CM-ID")).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.empty());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenRequestIdIsEmpty() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        Map<String, Object> body = new HashMap<>();
        when(requestEntity.getBody()).thenReturn(new ObjectMapper().writeValueAsString(body));
        String testCmId = "testCMId";
        when(httpHeaders.get("X-CM-ID")).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenRequestIdMappingIsEmpty() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> respNode = new HashMap<>();
        String testRequestId = "testRequestId";
        respNode.put("requestId", testRequestId);
        body.put("resp",respNode);
        when(requestEntity.getBody()).thenReturn(new ObjectMapper().writeValueAsString(body));
        String testCmId = "testCMId";
        when(httpHeaders.get("X-CM-ID")).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.empty());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity))
                .verifyComplete();
    }
    @Test
    public void shouldReturnValidatedResponse() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testRequestId = "testRequestId";
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        ObjectNode respNode = new ObjectMapper().createObjectNode();
        respNode.put("requestId", testRequestId);
        objectNode.set("resp",respNode);
        respNode.put("requestId", testRequestId);
        when(requestEntity.getBody()).thenReturn(new ObjectMapper().writeValueAsString(objectNode));
        String testCmId = "testCMId";
        when(httpHeaders.get("X-CM-ID")).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));
        String cachedRequestId = "cachedRequestId";
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.just(cachedRequestId));

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity))
                .assertNext(validatedDiscoverResponse -> {
                    Assertions.assertEquals(cmConfig,validatedDiscoverResponse.getCmConfig());
                    Assertions.assertEquals(cachedRequestId,validatedDiscoverResponse.getCallerRequestId());
                    Assertions.assertEquals(objectNode,validatedDiscoverResponse.getDeserializedJsonNode());
                })
                .verifyComplete();
    }
//
//    @Test
//    public void shouldNotErrorNotifyWhenRequestIdIsNotPresent() throws JsonProcessingException {
//        Map<String,Object> requestBody = new HashMap<>();
//        requestBody.put("foo", "bar");
//        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));
//
//        StepVerifier.create(discoveryValidator.errorNotify(requestEntity,null,null))
//                .verifyComplete();
//    }
//
//    @Test
//    public void shouldNotErrorNotifyWhenTransactionIdIsNotPresent() throws JsonProcessingException {
//        Map<String,Object> requestBody = new HashMap<>();
//        requestBody.put("requestId", "testRequestId");
//        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));
//
//        StepVerifier.create(discoveryValidator.errorNotify(requestEntity,null,null))
//                .verifyComplete();
//    }
//
//    @Test
//    public void shouldNotifyError() throws JsonProcessingException {
//        Map<String,Object> requestBody = new HashMap<>();
//        requestBody.put("requestId", UUID.randomUUID());
//        requestBody.put("transactionId", UUID.randomUUID());
//        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));
//        String cmId = "testCmId";
//        when(cmRegistry.getConfigFor(cmId)).thenReturn(Optional.of(cmConfig));
//        String testCmHost = "testCmHost";
//        when(cmConfig.getHost()).thenReturn(testCmHost);
//        when(discoveryServiceClient.patientErrorResultNotify(discoveryResultArgumentCaptor.capture(),eq(testCmHost))).thenReturn(Mono.empty());
//
//        Error error = new Error();
//        StepVerifier.create(discoveryValidator.errorNotify(requestEntity,cmId,error))
//                .verifyComplete();
//
//        Assertions.assertEquals(error,discoveryResultArgumentCaptor.getValue().getError());
//
//    }
}