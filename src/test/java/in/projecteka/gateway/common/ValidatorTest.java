package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static in.projecteka.gateway.clients.ClientError.invalidRequest;
import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.registry.ServiceType.HIP;
import static in.projecteka.gateway.registry.ServiceType.HIU;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestBuilders.yamlRegistryMapping;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ValidatorTest {

    @Mock
    HttpEntity<String> requestEntity;

    @Mock
    HttpHeaders httpHeaders;

    Validator validator;

    @Mock
    BridgeRegistry bridgeRegistry;

    @Mock
    CMRegistry cmRegistry;

    @Mock
    CacheAdapter<String, String> requestIdMappings;

    @Mock
    YamlRegistryMapping cmConfig;

    static Stream<Arguments> bridgeConfigs() {
        return Stream.of(Arguments.of(X_HIP_ID, HIP), Arguments.of(X_HIU_ID, HIU));
    }

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        validator = Mockito.spy(new Validator(bridgeRegistry, cmRegistry, requestIdMappings));
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIP_ID, X_HIU_ID})
    void returnErrorWhenHeaderIsNotPresent(String routingKey) {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(routingKey)).thenReturn(emptyList());

        StepVerifier.create(validator.validateRequest(requestEntity, routingKey))
                .verifyErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(mappingNotFoundForId(routingKey)));
    }

    @ParameterizedTest
    @ValueSource(strings = {X_HIP_ID, X_HIU_ID})
    void returnErrorWhenNoMappingIsFoundForBridges(String routingKey) {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.getFirst(routingKey)).thenReturn(string());

        StepVerifier.create(validator.validateRequest(requestEntity, routingKey))
                .verifyErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(mappingNotFoundForId(routingKey)));
    }

    @ParameterizedTest
    @MethodSource("bridgeConfigs")
    void returnErrorWhenNoRequestIdIsFound(String routingKey, ServiceType serviceType) throws JsonProcessingException {
        var bridgeId = string();
        var bridgeConfig = yamlRegistryMapping().build();
        Map<String, Object> requestBody = new HashMap<>();
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(requestBody));
        when(httpHeaders.getFirst(routingKey)).thenReturn(bridgeId);
        when(bridgeRegistry.getConfigFor(bridgeId, serviceType)).thenReturn(of(bridgeConfig));

        StepVerifier.create(validator.validateRequest(requestEntity, routingKey))
                .verifyErrorSatisfies(throwable ->
                {
                    var error = invalidRequest("Empty/Invalid requestId found on the payload");
                    assertThat(throwable).isEqualToComparingFieldByField(error);
                });
    }

    @ParameterizedTest
    @MethodSource("bridgeConfigs")
    void returnErrorWhenNoRequestIdIsInvalidUUID(String routingKey, ServiceType serviceType)
            throws JsonProcessingException {
        var bridgeConfig = yamlRegistryMapping().build();
        var requestBody = Map.of(REQUEST_ID, string());
        var bridgeId = string();
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(requestBody));
        when(httpHeaders.getFirst(routingKey)).thenReturn(bridgeId);
        when(bridgeRegistry.getConfigFor(bridgeId, serviceType)).thenReturn(of(bridgeConfig));

        StepVerifier.create(validator.validateRequest(requestEntity, routingKey))
                .verifyErrorSatisfies(throwable ->
                {
                    var error = invalidRequest("Empty/Invalid requestId found on the payload");
                    assertThat(throwable).isEqualToComparingFieldByField(error);
                });
    }

    @ParameterizedTest
    @MethodSource("bridgeConfigs")
    void returnValidatedRequest(String routingKey, ServiceType serviceType) throws JsonProcessingException {
        var requestId = UUID.randomUUID();
        var bridgeConfig = yamlRegistryMapping().build();
        var bridgeId = string();
        var requestBody = Map.of(REQUEST_ID, requestId.toString());
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(requestBody));
        when(httpHeaders.getFirst(routingKey)).thenReturn(bridgeId);
        when(bridgeRegistry.getConfigFor(bridgeId, serviceType)).thenReturn(of(bridgeConfig));

        StepVerifier.create(validator.validateRequest(requestEntity, routingKey))
                .assertNext(validatedDiscoverRequest -> {
                    assertThat(bridgeConfig).isEqualTo(validatedDiscoverRequest.getConfig());
                    assertThat(requestBody).isEqualTo(validatedDiscoverRequest.getDeSerializedRequest());
                    assertThat(requestId).isEqualTo(validatedDiscoverRequest.getRequesterRequestId());
                })
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {X_CM_ID, X_HIP_ID, X_HIU_ID})
    void shouldReturnEmptyWhenCMHeaderIsNotPresent(String routingKey) {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(routingKey)).thenReturn(emptyList());

        StepVerifier.create(validator.validateResponse(requestEntity, routingKey))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(mappingNotFoundForId(routingKey)))
                .verify();
    }

    @Test
    void shouldReturnEmptyWhenNoMappingIsFoundForCMId() {
        var testCmId = string();
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(X_CM_ID)).thenReturn(Collections.singletonList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.empty());

        StepVerifier.create(validator.validateResponse(requestEntity, X_CM_ID))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(mappingNotFoundForId(X_CM_ID)))
                .verify();
    }

    @Test
    void shouldReturnEmptyWhenRequestIdIsEmpty() throws JsonProcessingException {
        var testCmId = string();
        Map<String, Object> body = Map.of();
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(body));
        when(httpHeaders.get(X_CM_ID)).thenReturn(Collections.singletonList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(of(cmConfig));

        StepVerifier.create(validator.validateResponse(requestEntity, X_CM_ID))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(mappingNotFoundForId(X_CM_ID)))
                .verify();
    }

    @Test
    void shouldReturnEmptyWhenRequestIdMappingIsEmpty() throws JsonProcessingException {
        var testCmId = string();
        var testRequestId = string();
        var respNode = Map.of(REQUEST_ID, testRequestId);
        var body = Map.of("resp", respNode);
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(body));
        when(httpHeaders.get(X_CM_ID)).thenReturn(Collections.singletonList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(of(cmConfig));
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.empty());

        StepVerifier.create(validator.validateResponse(requestEntity, X_CM_ID))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(mappingNotFoundForId(X_CM_ID)))
                .verify();
    }

    @Test
    void shouldReturnValidatedResponse() throws JsonProcessingException {
        var testRequestId = string();
        var objectNode = OBJECT_MAPPER.createObjectNode();
        var respNode = OBJECT_MAPPER.createObjectNode();
        var testCmId = string();
        var cachedRequestId = string();
        respNode.put(REQUEST_ID, testRequestId);
        objectNode.set("resp", respNode);
        respNode.put(REQUEST_ID, testRequestId);
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(objectNode));
        when(httpHeaders.getFirst(X_CM_ID)).thenReturn(testCmId);
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(of(cmConfig));
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.just(cachedRequestId));

        StepVerifier.create(validator.validateResponse(requestEntity, X_CM_ID))
                .assertNext(validatedDiscoverResponse -> {
                    Assertions.assertEquals(testCmId, validatedDiscoverResponse.getId());
                    Assertions.assertEquals(cachedRequestId, validatedDiscoverResponse.getCallerRequestId());
                    Assertions.assertEquals(objectNode, validatedDiscoverResponse.getDeSerializedJsonNode());
                })
                .verifyComplete();
    }
}