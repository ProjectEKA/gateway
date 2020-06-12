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
import static in.projecteka.gateway.testcommon.TestBuilders.yamlRegistryMapping;
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

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIU_ID, X_HIP_ID})
    void returnErrorWhenValidationFails(String routingKey) throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID().toString();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(error(mappingNotFoundForId(routingKey)));

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, clientId))
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
        var host = string();
        var registryMapping = yamlRegistryMapping().host(host).build();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(registryMapping, requestId, requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId.toString()))).thenReturn(empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host))).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, string()))
                .verifyComplete();

        Assertions.assertEquals(requestIdCaptor.getValue(), captor.getValue().get(REQUEST_ID).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIU_ID, X_HIP_ID})
    void notifyCallerAboutTimeoutException(String routingKey) throws JsonProcessingException {
        var clientId = string();
        var requestId = UUID.randomUUID();
        var requestBody = new HashMap<String, Object>(Map.of(REQUEST_ID, requestId));
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(requestBody));
        var host = string();
        var registryMapping = yamlRegistryMapping().host(host).build();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(registryMapping, requestId, requestBody)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host)))
                .thenReturn(error(new TimeoutException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), errorResult.capture())).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, clientId))
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
        var host = string();
        var registryMapping = yamlRegistryMapping().host(host).build();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(registryMapping, requestId, requestBody)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host)))
                .thenReturn(error(new RuntimeException()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), errorResult.capture())).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, clientId))
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
        var host = string();
        var registryMapping = yamlRegistryMapping().host(host).build();
        when(discoveryValidator.validateRequest(requestEntity, routingKey))
                .thenReturn(just(new ValidatedRequest(registryMapping, requestId, requestBody)));
        when(requestIdMappings.put(any(), eq(requestId.toString()))).thenReturn(empty());
        when(discoveryServiceClient.routeRequest(captor.capture(), eq(host)))
                .thenReturn(error(ClientError.unableToConnect()));
        var errorResult = ArgumentCaptor.forClass(ErrorResult.class);
        when(discoveryServiceClient.notifyError(eq(clientId), errorResult.capture())).thenReturn(empty());

        StepVerifier.create(requestOrchestrator.handleThis(requestEntity, routingKey, clientId))
                .verifyComplete();

        verify(discoveryValidator).validateRequest(requestEntity, routingKey);
        assertThat(errorResult.getValue().getResp().getRequestId()).isEqualTo(requestId);
        assertThat(errorResult.getValue().getError().getMessage())
                .isEqualTo("Cannot process the request at the moment, please try later.");
        assertThat(errorResult.getValue().getError().getCode()).isEqualTo(UNKNOWN_ERROR_OCCURRED);
    }
}
