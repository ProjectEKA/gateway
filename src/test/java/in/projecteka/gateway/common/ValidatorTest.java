package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ValidatorTest {

    @Mock
    HttpEntity<String> requestEntity;
    @Mock
    HttpHeaders httpHeaders;
    Validator discoveryValidator;
    @Mock BridgeRegistry bridgeRegistry;
    @Mock
    CMRegistry cmRegistry;
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
        when(httpHeaders.get(X_HIP_ID)).thenReturn(Collections.emptyList());

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .verifyErrorSatisfies(throwable -> assertThat(throwable)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class)).isEqualToComparingFieldByField(ClientError.idMissingInHeader(X_HIP_ID)));
    }

    @Test
    public void shouldThrowErrorWhenNoMappingIsFoundForHIPId() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testHipId = "testHipId";
        when(httpHeaders.getFirst(X_HIP_ID)).thenReturn(testHipId);
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.empty());

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .verifyErrorSatisfies(throwable -> assertThat(throwable)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class)).isEqualToComparingFieldByField(ClientError.mappingNotFoundForId(X_HIP_ID)));

    }

    @Test
    public void shouldReturnEmptyWhenNoRequestIdIsFound() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        Map<String,Object> requestBody = new HashMap<>();
        when(requestEntity.getBody()).thenReturn(new ObjectMapper().writeValueAsString(requestBody));
        String testHipId = "testHipId";
        when(httpHeaders.getFirst(X_HIP_ID)).thenReturn(testHipId);
        YamlRegistryMapping hipConfig = new YamlRegistryMapping();
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
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
        when(httpHeaders.getFirst(X_HIP_ID)).thenReturn(testHipId);
        YamlRegistryMapping hipConfig = new YamlRegistryMapping();
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .assertNext(validatedDiscoverRequest -> {
                    assertThat(hipConfig).isEqualTo(validatedDiscoverRequest.getConfig());
                    assertThat(requestBody).isEqualTo(validatedDiscoverRequest.getDeserializedRequest());
                    assertThat(cmRequestId).isEqualTo(validatedDiscoverRequest.getRequesterRequestId());
                }) .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenCMHeaderIsNotPresent() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(X_CM_ID)).thenReturn(Collections.emptyList());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenNoMappingIsFoundForCMId() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testCmId = "testCMId";
        when(httpHeaders.get(X_CM_ID)).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.empty());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenRequestIdIsEmpty() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        Map<String, Object> body = new HashMap<>();
        when(requestEntity.getBody()).thenReturn(new ObjectMapper().writeValueAsString(body));
        String testCmId = "testCMId";
        when(httpHeaders.get(X_CM_ID)).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
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
        when(httpHeaders.get(X_CM_ID)).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.empty());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
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
        when(httpHeaders.getFirst(X_CM_ID)).thenReturn(testCmId);
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));
        String cachedRequestId = "cachedRequestId";
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.just(cachedRequestId));

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .assertNext(validatedDiscoverResponse -> {
                    Assertions.assertEquals(testCmId,validatedDiscoverResponse.getId());
                    Assertions.assertEquals(cachedRequestId,validatedDiscoverResponse.getCallerRequestId());
                    Assertions.assertEquals(objectNode,validatedDiscoverResponse.getDeserializedJsonNode());
                })
                .verifyComplete();
    }
}