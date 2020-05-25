package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscoveryHelperTest {
    @Mock
    DiscoveryValidator discoveryValidator;
    @InjectMocks
    DiscoveryHelper discoveryHelper;
    @Mock
    YamlRegistryMapping hipConfig;
    @Mock
    YamlRegistryMapping cmConfig;
    @Mock
    CacheAdapter<String,String> requestIdMappings;
    @Mock
    DiscoveryServiceClient discoveryServiceClient;
    private @Captor ArgumentCaptor<Map<String,Object>> captor;
    private @Captor ArgumentCaptor<String> requestIdCaptor;
    private @Captor ArgumentCaptor<Error> errorArgumentCaptor;
    private @Captor ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;
    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldNotCallBridgeOnValidationFailure() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        when(discoveryValidator.validateDiscoverRequest(requestEntity)).thenReturn(Mono.empty());

        StepVerifier.create(discoveryHelper.doDiscoverCareContext(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldNotCallCMonValidationErrors() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        when(discoveryValidator.validateDiscoverResponse(requestEntity)).thenReturn(Mono.empty());

        StepVerifier.create(discoveryHelper.doOnDiscoverCareContext(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldNotCallBridgeWhenRequestIdIsNotPresent() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("foo", "bar");
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody)
);
        when(discoveryValidator.validateDiscoverRequest(requestEntity)).thenReturn(Mono.just(new ValidatedDiscoverRequest(null)));

        StepVerifier.create(discoveryHelper.doDiscoverCareContext(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldNotCallCMWhenRequestIdIsNotPresent() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("foo", "bar");
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody)
        );
        when(discoveryValidator.validateDiscoverResponse(requestEntity)).thenReturn(Mono.just(new ValidatedDiscoverResponse(null)));

        StepVerifier.create(discoveryHelper.doOnDiscoverCareContext(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldCallBridgeWhenValidRequest() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateDiscoverRequest(requestEntity)).thenReturn(Mono.just(new ValidatedDiscoverRequest(hipConfig)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.patientFor(captor.capture(),eq(testhost))).thenReturn(Mono.empty());

        StepVerifier.create(discoveryHelper.doDiscoverCareContext(requestEntity))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateDiscoverRequest(requestEntity);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
    }

    @Test
    public void shouldCallCMWhenValidRequest() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        String cmRequestId = UUID.randomUUID().toString();
        Map<String,Object> requestIdNode = new HashMap<>();
        requestIdNode.put("requestId", requestId);
        requestBody.put("resp", requestIdNode);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(cmConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateDiscoverResponse(requestEntity)).thenReturn(Mono.just(new ValidatedDiscoverResponse(cmConfig)));
        when(requestIdMappings.get(eq(requestId))).thenReturn(Mono.just(cmRequestId));
        when(discoveryServiceClient.patientDiscoveryResultNotify(jsonNodeArgumentCaptor.capture(),eq(testhost))).thenReturn(Mono.empty());

        StepVerifier.create(discoveryHelper.doOnDiscoverCareContext(requestEntity))
                .verifyComplete();

        verify(cmConfig).getHost();
        verify(discoveryValidator).validateDiscoverResponse(requestEntity);
        Assertions.assertEquals(cmRequestId,jsonNodeArgumentCaptor.getValue().path("resp").path("requestId").asText());
    }

    @Test
    public void shouldNotCallBridgeWhenRequestIdMappingisNotFound() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        Map<String,Object> requestIdNode = new HashMap<>();
        requestIdNode.put("requestId", requestId);
        requestBody.put("resp", requestIdNode);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        when(discoveryValidator.validateDiscoverResponse(requestEntity)).thenReturn(Mono.just(new ValidatedDiscoverResponse(null)));
        when(requestIdMappings.get(eq(requestId))).thenReturn(Mono.empty());

        StepVerifier.create(discoveryHelper.doOnDiscoverCareContext(requestEntity))
                .verifyComplete();

        verify(discoveryValidator).validateDiscoverResponse(requestEntity);
    }

    @Test
    public void shouldNotifyCMOnBridgeTimeout() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateDiscoverRequest(requestEntity)).thenReturn(Mono.just(new ValidatedDiscoverRequest(hipConfig)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.patientFor(captor.capture(),eq(testhost))).thenReturn(Mono.error(new TimeoutException()));
        when(discoveryValidator.errorNotify(eq(requestEntity),eq(Constants.TEMP_CM_ID),errorArgumentCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(discoveryHelper.doDiscoverCareContext(requestEntity))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateDiscoverRequest(requestEntity);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
        Assertions.assertEquals("Timedout When calling bridge",errorArgumentCaptor.getValue().getMessage());
    }

    @Test
    public void shouldNotifyCMOnBridgeError() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateDiscoverRequest(requestEntity)).thenReturn(Mono.just(new ValidatedDiscoverRequest(hipConfig)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.patientFor(captor.capture(),eq(testhost))).thenReturn(Mono.error(new RuntimeException()));
        when(discoveryValidator.errorNotify(eq(requestEntity),eq(Constants.TEMP_CM_ID),errorArgumentCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(discoveryHelper.doDiscoverCareContext(requestEntity))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateDiscoverRequest(requestEntity);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
        Assertions.assertEquals("Error in making call to Bridge",errorArgumentCaptor.getValue().getMessage());
    }

}