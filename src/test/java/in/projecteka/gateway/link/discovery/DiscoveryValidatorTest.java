package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.link.common.Constants;
import in.projecteka.gateway.link.discovery.model.PatientDiscoveryResult;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DiscoveryValidatorTest {

    @Mock
    HttpEntity<String> requestEntity;
    @Mock
    HttpHeaders httpHeaders;
    DiscoveryValidator discoveryValidator;
    @Captor
    ArgumentCaptor<Error> errorArgumentCaptor;
    @Captor
    ArgumentCaptor<PatientDiscoveryResult> discoveryResultArgumentCaptor;
    @Mock BridgeRegistry bridgeRegistry;
    @Mock
    CMRegistry cmRegistry;
    @Mock
    DiscoveryServiceClient discoveryServiceClient;
    @Mock
    YamlRegistryMapping cmConfig;
    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        discoveryValidator = Mockito.spy(new DiscoveryValidator(bridgeRegistry,cmRegistry,discoveryServiceClient));
    }

    @Test
    public void shouldErrorNotifyWhenHIPHeaderIsNotPresent() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get("X-HIP-ID")).thenReturn(Collections.emptyList());
        Mockito.doReturn(Mono.empty()).when(discoveryValidator).errorNotify(eq(requestEntity),eq(Constants.TEMP_CM_ID),errorArgumentCaptor.capture());

        StepVerifier.create(discoveryValidator.validateDiscoverRequest(requestEntity))
                .verifyComplete();

        Assertions.assertEquals("X-HIP-ID missing on headers",errorArgumentCaptor.getValue().getMessage());
        Mockito.verify(discoveryValidator).errorNotify(eq(requestEntity),eq(Constants.TEMP_CM_ID),any(Error.class));
    }

    @Test
    public void shouldErrorNotifyWhenNoMappingIsFoundForHIPId() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testHipId = "testHipId";
        when(httpHeaders.get("X-HIP-ID")).thenReturn(Arrays.asList(testHipId));
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.empty());
        Mockito.doReturn(Mono.empty()).when(discoveryValidator).errorNotify(eq(requestEntity),eq(Constants.TEMP_CM_ID),errorArgumentCaptor.capture());

        StepVerifier.create(discoveryValidator.validateDiscoverRequest(requestEntity))
                .verifyComplete();

        Assertions.assertEquals("No mapping found for X-HIP-ID",errorArgumentCaptor.getValue().getMessage());
    }

    @Test
    public void shouldReturnValidatedRequest() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testHipId = "testHipId";
        when(httpHeaders.get("X-HIP-ID")).thenReturn(Arrays.asList(testHipId));
        YamlRegistryMapping hipConfig = new YamlRegistryMapping();
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateDiscoverRequest(requestEntity))
                .assertNext(validatedDiscoverRequest -> Assertions.assertEquals(hipConfig,validatedDiscoverRequest.getHipConfig()))
                .verifyComplete();
    }
    @Test
    public void shouldReturnEmptyWhenCMHeaderIsNotPresent() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get("X-CM-ID")).thenReturn(Collections.emptyList());

        StepVerifier.create(discoveryValidator.validateDiscoverResponse(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldReturnEmptyWhenNoMappingIsFoundForCMId() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testCmId = "testCMId";
        when(httpHeaders.get("X-CM-ID")).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.empty());

        StepVerifier.create(discoveryValidator.validateDiscoverResponse(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldReturnValidatedResponse() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testCmId = "testCMId";
        when(httpHeaders.get("X-CM-ID")).thenReturn(Arrays.asList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));

        StepVerifier.create(discoveryValidator.validateDiscoverResponse(requestEntity))
                .assertNext(validatedDiscoverResponse -> Assertions.assertEquals(cmConfig,validatedDiscoverResponse.getCmConfig()))
                .verifyComplete();
    }

    @Test
    public void shouldNotErrorNotifyWhenRequestIdIsNotPresent() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("foo", "bar");
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        StepVerifier.create(discoveryValidator.errorNotify(requestEntity,null,null))
                .verifyComplete();
    }

    @Test
    public void shouldNotErrorNotifyWhenTransactionIdIsNotPresent() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("requestId", "testRequestId");
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        StepVerifier.create(discoveryValidator.errorNotify(requestEntity,null,null))
                .verifyComplete();
    }

    @Test
    public void shouldNotifyError() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("requestId", UUID.randomUUID());
        requestBody.put("transactionId", UUID.randomUUID());
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));
        String cmId = "testCmId";
        when(cmRegistry.getConfigFor(cmId)).thenReturn(Optional.of(cmConfig));
        String testCmHost = "testCmHost";
        when(cmConfig.getHost()).thenReturn(testCmHost);
        when(discoveryServiceClient.patientErrorResultNotify(discoveryResultArgumentCaptor.capture(),eq(testCmHost))).thenReturn(Mono.empty());

        Error error = new Error();
        StepVerifier.create(discoveryValidator.errorNotify(requestEntity,cmId,error))
                .verifyComplete();

        Assertions.assertEquals(error,discoveryResultArgumentCaptor.getValue().getError());

    }
}