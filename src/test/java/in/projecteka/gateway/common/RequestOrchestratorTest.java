package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.model.ErrorResult;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.TestBuilders.string;
import static in.projecteka.gateway.common.TestEssentials.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestOrchestratorTest {

    @Mock
    Validator discoveryValidator;

    @Mock
    YamlRegistryMapping hipConfig;

    @Mock
    CacheAdapter<String, String> requestIdMappings;

    @Mock
    DiscoveryServiceClient discoveryServiceClient;

    @Captor
    ArgumentCaptor<Map<String, Object>> captor;

    @Captor
    ArgumentCaptor<String> requestIdCaptor;

    RequestOrchestrator requestOrchestrator;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        requestOrchestrator = Mockito.spy(new RequestOrchestrator(requestIdMappings,
                discoveryValidator,
                discoveryServiceClient));
    }

    /*
     TODO: We should have return failure immediately when there is no CM_ID, instead of planning to send
     error through error notify.
    */
    @Test
    void shouldDoNothingWhenRequestIdIsInvalidOrEmpty() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyComplete();
    }

    @Test
    void shouldNotifyErrorWhenValidationFails() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        Map<String, Object> requestBody = Map.of(REQUEST_ID, requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        ClientError clientError = ClientError.idMissingInHeader(X_CM_ID);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.error(clientError));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(errorResult.capture())).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyError(ErrorResult.class);

        assertThat(errorResult.getValue().getError().getMessage()).isEqualTo("X-CM-ID missing in headers");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }

    @Test
    void shouldCallBridgeWhenValidRequest() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        Map<String, Object> requestBody = new HashMap<>(Map.of(REQUEST_ID, requestId));
        HttpEntity<String> requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        String host = string();
        when(hipConfig.getHost()).thenReturn(host);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.just(new ValidatedRequest(hipConfig, requestId, requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host))).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyComplete();

        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
        Assertions.assertEquals(requestIdCaptor.getValue(), captor.getValue().get(REQUEST_ID).toString());
    }

    @Test
    void shouldNotifyCMOnBridgeTimeout() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        var host = string();
        when(hipConfig.getHost()).thenReturn(host);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.just(new ValidatedRequest(hipConfig, requestId, requestBody)));
        when(requestIdMappings.put(any(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host)))
                .thenReturn(Mono.error(new TimeoutException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(errorResult.capture())).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyError(ErrorResult.class);

        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
        assertThat(errorResult.getValue().getResp().getRequestId().toString()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage()).isEqualTo("Timed out When calling target system");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }

    @Test
    void shouldNotifyCMOnBridgeError() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        Map<String, Object> requestBody = Map.of(REQUEST_ID, requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        String host = string();
        when(hipConfig.getHost()).thenReturn(host);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.just(new ValidatedRequest(hipConfig, requestId, requestBody)));
        when(requestIdMappings.put(any(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host)))
                .thenReturn(Mono.error(new RuntimeException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(errorResult.capture())).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyError(ErrorResult.class);

        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
        assertThat(errorResult.getValue().getResp().getRequestId().toString()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage()).isEqualTo("Error in making call to target system");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }
}
