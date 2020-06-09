package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestBuilders.yamlRegistryMapping;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ValidatorTest {

    @Mock
    HttpEntity<String> requestEntity;

    @Mock
    HttpHeaders httpHeaders;

    Validator discoveryValidator;
    @Mock
    BridgeRegistry bridgeRegistry;

    @Mock
    CMRegistry cmRegistry;

    @Mock
    CacheAdapter<String, String> requestIdMappings;

    @Mock
    YamlRegistryMapping cmConfig;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        discoveryValidator = Mockito.spy(new Validator(bridgeRegistry, cmRegistry, requestIdMappings));
    }

    @Test
    void shouldThrowErrorWhenHIPHeaderIsNotPresent() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(X_HIP_ID)).thenReturn(Collections.emptyList());

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .verifyErrorSatisfies(throwable -> assertThat(throwable)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class))
                        .isEqualToComparingFieldByField(ClientError.idMissingInHeader(X_HIP_ID)));
    }

    @Test
    void shouldThrowErrorWhenNoMappingIsFoundForHIPId() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        String testHipId = string();
        when(httpHeaders.getFirst(X_HIP_ID)).thenReturn(testHipId);
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.empty());

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .verifyErrorSatisfies(throwable -> assertThat(throwable)
                        .asInstanceOf(InstanceOfAssertFactories.type(ClientError.class))
                        .isEqualToComparingFieldByField(ClientError.mappingNotFoundForId(X_HIP_ID)));

    }

    @Test
    void returnErrorWhenNoRequestIdIsFound() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        Map<String, Object> requestBody = new HashMap<>();
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(requestBody));
        String testHipId = string();
        when(httpHeaders.getFirst(X_HIP_ID)).thenReturn(testHipId);
        var hipConfig = yamlRegistryMapping().build();
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .expectErrorSatisfies(throwable ->
                {
                    var error = ClientError.invalidRequest("Empty/Invalid requestId found on the payload");
                    assertThat(throwable).isEqualToComparingFieldByField(error);
                })
                .verify();
    }

    @Test
    void returnErrorWhenNoRequestIdIsInvalidUUID() throws JsonProcessingException {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        var requestBody = Map.of(REQUEST_ID, string());
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(requestBody));
        String testHipId = string();
        when(httpHeaders.getFirst(X_HIP_ID)).thenReturn(testHipId);
        var hipConfig = yamlRegistryMapping().build();
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .expectErrorSatisfies(throwable ->
                {
                    var error = ClientError.invalidRequest("Empty/Invalid requestId found on the payload");
                    assertThat(throwable).isEqualToComparingFieldByField(error);
                })
                .verify();
    }

    @Test
    void shouldReturnValidatedRequest() throws JsonProcessingException {
        var cmRequestId = UUID.randomUUID();
        var hipConfig = yamlRegistryMapping().build();
        var testHipId = string();
        var requestBody = Map.of(REQUEST_ID, cmRequestId.toString());
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(requestBody));
        when(httpHeaders.getFirst(X_HIP_ID)).thenReturn(testHipId);
        when(bridgeRegistry.getConfigFor(testHipId, ServiceType.HIP)).thenReturn(Optional.of(hipConfig));

        StepVerifier.create(discoveryValidator.validateRequest(requestEntity, X_HIP_ID))
                .assertNext(validatedDiscoverRequest -> {
                    assertThat(hipConfig).isEqualTo(validatedDiscoverRequest.getConfig());
                    assertThat(requestBody).isEqualTo(validatedDiscoverRequest.getDeSerializedRequest());
                    assertThat(cmRequestId).isEqualTo(validatedDiscoverRequest.getRequesterRequestId());
                }).verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenCMHeaderIsNotPresent() {
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(X_CM_ID)).thenReturn(Collections.emptyList());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenNoMappingIsFoundForCMId() {
        var testCmId = string();
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(X_CM_ID)).thenReturn(Collections.singletonList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.empty());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenRequestIdIsEmpty() throws JsonProcessingException {
        var testCmId = string();
        Map<String, Object> body = Map.of();
        when(requestEntity.getHeaders()).thenReturn(httpHeaders);
        when(requestEntity.getBody()).thenReturn(OBJECT_MAPPER.writeValueAsString(body));
        when(httpHeaders.get(X_CM_ID)).thenReturn(Collections.singletonList(testCmId));
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .verifyComplete();
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
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.empty());

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .verifyComplete();
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
        when(cmRegistry.getConfigFor(testCmId)).thenReturn(Optional.of(cmConfig));
        when(requestIdMappings.get(testRequestId)).thenReturn(Mono.just(cachedRequestId));

        StepVerifier.create(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .assertNext(validatedDiscoverResponse -> {
                    Assertions.assertEquals(testCmId, validatedDiscoverResponse.getId());
                    Assertions.assertEquals(cachedRequestId, validatedDiscoverResponse.getCallerRequestId());
                    Assertions.assertEquals(objectNode, validatedDiscoverResponse.getDeserializedJsonNode());
                })
                .verifyComplete();
    }
}