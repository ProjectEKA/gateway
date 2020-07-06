package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.model.ErrorResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

class RequestOrchestratorTest {

    @Mock
    Validator discoveryValidator;

    @Mock
    CacheAdapter<String, String> requestIdMappings;

    @Mock
    CacheAdapter<String, String> requestIdValidator;

    @Mock
    DiscoveryServiceClient discoveryServiceClient;

    @Captor
    ArgumentCaptor<Map<String, Object>> captor;

    @Captor
    ArgumentCaptor<String> requestIdCaptor;

    RequestOrchestrator<?> requestOrchestrator;

    @Mock
    ValidatedRequestAction validatedRequestAction;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        requestOrchestrator = Mockito.spy(new RequestOrchestrator<>(requestIdMappings,
                requestIdValidator,
                discoveryValidator,
                discoveryServiceClient,
                validatedRequestAction));
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIU_ID, X_HIP_ID})
    void returnErrorWhenValidationFails(String routingKey) throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID().toString();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(error(mappingNotFoundForId(routingKey)));

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, routingKey, clientId))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(mappingNotFoundForId(routingKey)))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIU_ID, X_HIP_ID})
    void callDownStreamSystemForAValidRequest(String routingKey) throws JsonProcessingException {
        var requestId = UUID.randomUUID();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId.toString()));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        var targetClientId = string();
        String clientId = string();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(requestId, requestBody, targetClientId)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId.toString()))).thenReturn(empty());
        when(validatedRequestAction.execute(eq(targetClientId),captor.capture(), eq(routingKey))).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, routingKey, clientId)).verifyComplete();
        Assertions.assertEquals(requestIdCaptor.getValue(), captor.getValue().get(REQUEST_ID).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIU_ID, X_HIP_ID})
    void notifyCallerAboutTimeoutException(String routingKey) throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        var targetClientId = string();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(requestId, requestBody, targetClientId)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(empty());
        when(validatedRequestAction.execute(eq(targetClientId),captor.capture(), eq(routingKey))).thenReturn(error(new TimeoutException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), eq(routingKey), errorResult.capture())).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, routingKey, clientId))
                .verifyComplete();

        verify(discoveryValidator).validateRequest(requestEntity, routingKey);
        assertThat(errorResult.getValue().getResp().getRequestId()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage()).isEqualTo("Timed out When calling target system");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIU_ID, X_HIP_ID})
    void notifyCallerAboutUnknownFailure(String routingKey) throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        HttpEntity<String> requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        var targetClientId = string();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(requestId, requestBody, targetClientId)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(empty());
        when(validatedRequestAction.execute(eq(targetClientId),captor.capture(), eq(routingKey))).thenReturn(error(new RuntimeException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), eq(routingKey), errorResult.capture())).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, routingKey, clientId))
                .verifyComplete();

        verify(discoveryValidator).validateRequest(requestEntity, routingKey);
        assertThat(errorResult.getValue().getResp().getRequestId()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage()).isEqualTo("Error in making call to target system");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIU_ID, X_HIP_ID})
    void notifyCallerAboutClientError(String routingKey) throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        HttpEntity<String> requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        var targetClientId = string();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(requestId, requestBody, targetClientId)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(empty());
        when(validatedRequestAction.execute(eq(targetClientId),captor.capture(), eq(routingKey))).thenReturn(error(ClientError.unableToConnect()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), eq(routingKey), errorResult.capture())).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, routingKey, clientId))
                .verifyComplete();

        verify(discoveryValidator).validateRequest(requestEntity, routingKey);
        assertThat(errorResult.getValue().getResp().getRequestId()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage())
                .isEqualTo("Cannot process the request at the moment, please try later.");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }
}
