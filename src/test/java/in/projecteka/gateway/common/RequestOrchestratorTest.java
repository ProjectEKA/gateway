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
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
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

    RequestOrchestrator<?> requestOrchestrator;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        requestOrchestrator = Mockito.spy(new RequestOrchestrator<>(requestIdMappings,
                discoveryValidator,
                discoveryServiceClient));
    }

    @Test
    void returnErrorWhenValidationFails() throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID().toString();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.error(ClientError.idMissingInHeader(X_CM_ID)));

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID, clientId))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(ClientError.idMissingInHeader(X_CM_ID)))
                .verify();
    }

    @Test
    void shouldCallBridgeWhenValidRequest() throws JsonProcessingException {
        var requestId = UUID.randomUUID();
        Map<String, Object> requestBody = new HashMap<>(Map.of(REQUEST_ID, requestId.toString()));
        HttpEntity<String> requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        String host = string();
        when(hipConfig.getHost()).thenReturn(host);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.just(new ValidatedRequest(hipConfig, requestId, requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId.toString()))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host))).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID, string()))
                .verifyComplete();

        Assertions.assertEquals(requestIdCaptor.getValue(), captor.getValue().get(REQUEST_ID).toString());
    }

    @Test
    void shouldNotifyCMOnBridgeTimeout() throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        var host = string();
        when(hipConfig.getHost()).thenReturn(host);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.just(new ValidatedRequest(hipConfig, requestId, requestBody)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host)))
                .thenReturn(Mono.error(new TimeoutException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), errorResult.capture())).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID, clientId))
                .verifyComplete();

        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
        assertThat(errorResult.getValue().getResp().getRequestId()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage()).isEqualTo("Timed out When calling target system");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }

    @Test
    void shouldNotifyCMOnBridgeError() throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        HttpEntity<String> requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        String host = string();
        when(hipConfig.getHost()).thenReturn(host);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID))
                .thenReturn(Mono.just(new ValidatedRequest(hipConfig, requestId, requestBody)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host)))
                .thenReturn(Mono.error(new RuntimeException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), errorResult.capture())).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID, clientId))
                .verifyComplete();

        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
        assertThat(errorResult.getValue().getResp().getRequestId()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage()).isEqualTo("Error in making call to target system");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }
}
