package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.common.cache.CacheAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import reactor.test.StepVerifier;

import java.util.UUID;

import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

class ResponseOrchestratorTest {
    @Mock
    Validator validator;

    @Mock
    CacheAdapter<String, String> requestIdMappings;

    @Mock
    ValidatedResponseAction validatedResponseAction;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    ResponseOrchestrator responseOrchestrator;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        responseOrchestrator = Mockito.spy(new ResponseOrchestrator(validator, validatedResponseAction));
    }

    @Test
    public void shouldNotCallCMonValidationErrors() {
        var requestEntity = new HttpEntity<>("");
        var error = ClientError.invalidRequest("Invalid request");
        when(validator.validateResponse(requestEntity, X_CM_ID)).thenReturn(error(error));

        StepVerifier.create(responseOrchestrator.processResponse(requestEntity, X_CM_ID))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isEqualToComparingFieldByField(error);
                })
                .verify();
    }

    @Test
    public void shouldCallCMWhenValidRequest() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        var cmRequestId = UUID.randomUUID().toString();
        var objectNode = OBJECT_MAPPER.createObjectNode();
        var respNode = OBJECT_MAPPER.createObjectNode();
        var testCmId = string();
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(objectNode));
        objectNode.put(REQUEST_ID, requestId);
        respNode.put(REQUEST_ID, cmRequestId);
        objectNode.set("resp", respNode);
        when(validator.validateResponse(requestEntity, X_CM_ID))
                .thenReturn(just(new ValidatedResponse(testCmId, cmRequestId, objectNode)));
        when(requestIdMappings.get(eq(requestId))).thenReturn(just(cmRequestId));
        when(validatedResponseAction.execute(eq(X_CM_ID), eq(testCmId), jsonNodeArgumentCaptor.capture()))
                .thenReturn(empty());

        StepVerifier.create(responseOrchestrator.processResponse(requestEntity, X_CM_ID))
                .verifyComplete();

        verify(validator).validateResponse(requestEntity, X_CM_ID);
        Assertions.assertEquals(cmRequestId, jsonNodeArgumentCaptor.getValue().path("resp").path(REQUEST_ID).asText());
    }
}
