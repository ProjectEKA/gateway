package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.cache.CacheAdapter;
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

import java.util.UUID;

import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResponseOrchestratorTest {
    @Mock
    Validator discoveryValidator;
    @Mock
    YamlRegistryMapping cmConfig;
    @Mock
    CacheAdapter<String, String> requestIdMappings;
    @Mock
    ValidatedResponseAction validatedResponseAction;
    private @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;
    ResponseOrchestrator responseOrchestrator;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        responseOrchestrator = Mockito.spy(new ResponseOrchestrator(discoveryValidator, validatedResponseAction));
    }

    @Test
    public void shouldNotCallCMonValidationErrors() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        when(discoveryValidator.validateResponse(requestEntity, X_CM_ID)).thenReturn(Mono.empty());

        StepVerifier.create(responseOrchestrator.processResponse(requestEntity, X_CM_ID))
                .verifyComplete();
    }

    @Test
    public void shouldCallCMWhenValidRequest() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        var cmRequestId = UUID.randomUUID().toString();
        var objectNode = OBJECT_MAPPER.createObjectNode();
        var respNode = OBJECT_MAPPER.createObjectNode();
        var testCmId = string();
        objectNode.put(REQUEST_ID, requestId);
        respNode.put(REQUEST_ID, cmRequestId);
        objectNode.set("resp", respNode);
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(objectNode));
        when(discoveryValidator.validateResponse(requestEntity, X_CM_ID)).thenReturn(Mono.just(new ValidatedResponse(testCmId, cmRequestId, objectNode)));
        when(requestIdMappings.get(eq(requestId))).thenReturn(Mono.just(cmRequestId));
        when(validatedResponseAction.execute(eq(X_CM_ID), eq(testCmId), jsonNodeArgumentCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(responseOrchestrator.processResponse(requestEntity, X_CM_ID))
                .verifyComplete();

        verify(discoveryValidator).validateResponse(requestEntity, X_CM_ID);
        Assertions.assertEquals(cmRequestId, jsonNodeArgumentCaptor.getValue().path("resp").path(REQUEST_ID).asText());
    }

}
