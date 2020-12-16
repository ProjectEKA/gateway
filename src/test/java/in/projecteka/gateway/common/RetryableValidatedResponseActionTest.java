package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.impl.LongStringHelper;
import in.projecteka.gateway.clients.ServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;
import reactor.test.StepVerifier;

import java.util.UUID;

import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.common.Constants.GW_LINK_QUEUE;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.serviceOptions;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

class RetryableValidatedResponseActionTest {
    @Mock
    private Receiver receiver;

    @Mock
    private Sender sender;

    private JsonNode jsonNode;

    @Mock
    private DefaultValidatedResponseAction<ServiceClient> defaultValidatedResponseAction;

    private RetryableValidatedResponseAction<ServiceClient> retryableValidatedResponseAction;

    private AcknowledgableDelivery acknowledgableDelivery;

    @Captor
    private ArgumentCaptor<Mono<OutboundMessage>> outboundMessageCaptor;


    @BeforeEach
    public void init() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        jsonNode = Serializer.objectMapper.readValue("{}", JsonNode.class);
        acknowledgableDelivery = Mockito.mock(AcknowledgableDelivery.class, RETURNS_DEEP_STUBS);
        when(receiver.consumeManualAck(GW_LINK_QUEUE)).thenReturn(Flux.just(acknowledgableDelivery));
        retryableValidatedResponseAction = Mockito.spy(new RetryableValidatedResponseAction<>(receiver,
                sender,
                defaultValidatedResponseAction,
                serviceOptions().responseMaxRetryAttempts(5).retryAttemptsDelay(100).build(),
                GW_LINK_QUEUE,
                X_CM_ID));
    }

    @Test
    void shouldRouteResponseAndNotRetryWhenSuccessful() {

        LongString testCmId = LongStringHelper.asLongString("testCmId");
        var routingKey = X_CM_ID;

        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(UUID.randomUUID().toString())
                .message(jsonNode).build();

        when(acknowledgableDelivery.getProperties().getHeaders().get(routingKey)).thenReturn(testCmId);
        when(acknowledgableDelivery.getBody()).thenReturn(Serializer.from(traceableMessage).get().getBytes());

        doReturn(Mono.empty()).when(retryableValidatedResponseAction).routeResponse(testCmId.toString(), jsonNode, routingKey);

        StepVerifier.create(retryableValidatedResponseAction.processDelivery(acknowledgableDelivery))
                .verifyComplete();

        verify(retryableValidatedResponseAction).routeResponse(testCmId.toString(), jsonNode, routingKey);
    }

    @Test
    void shouldRetryIfFailsToRouteResponse() {
        String routingKey = X_CM_ID;
        LongString testCmId = LongStringHelper.asLongString("testCmId");

        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(UUID.randomUUID().toString())
                .message(jsonNode).build();

        when(acknowledgableDelivery.getProperties().getHeaders().get(routingKey)).thenReturn(testCmId);
        when(acknowledgableDelivery.getBody()).thenReturn(Serializer.from(traceableMessage).get().getBytes());

        doReturn(error(mappingNotFoundForId(testCmId.toString())), empty())
                .when(retryableValidatedResponseAction)
                .routeResponse(testCmId.toString(), jsonNode, routingKey);

        StepVerifier.create(retryableValidatedResponseAction.processDelivery(acknowledgableDelivery))
                .verifyComplete();

        verify(retryableValidatedResponseAction, times(2)).routeResponse(testCmId.toString(), jsonNode, routingKey);
    }

    @Test
    void shouldThrowExceptionWhenRetryLimitExceeds() {
        String routingKey = X_CM_ID;
        LongString testCmId = LongStringHelper.asLongString("testCmId");

        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(UUID.randomUUID().toString())
                .message(jsonNode).build();

        when(acknowledgableDelivery.getProperties().getHeaders().get(routingKey)).thenReturn(testCmId);
        when(acknowledgableDelivery.getBody()).thenReturn(Serializer.from(traceableMessage).get().getBytes());

        doReturn(error(mappingNotFoundForId(testCmId.toString())))
                .when(retryableValidatedResponseAction)
                .routeResponse(testCmId.toString(), jsonNode, routingKey);

        StepVerifier.create(retryableValidatedResponseAction.processDelivery(acknowledgableDelivery))
                .expectError(RetryLimitExceededException.class)
                .verify();

        verify(retryableValidatedResponseAction, times(6)).routeResponse(testCmId.toString(), jsonNode, routingKey);
    }

    @Test
    void shouldRouteResponse() {
        var testCmId = string();
        var routingKey = string();
        when(defaultValidatedResponseAction.routeResponse(testCmId, jsonNode, routingKey)).thenReturn(Mono.empty());

        StepVerifier.create(retryableValidatedResponseAction.routeResponse(testCmId, jsonNode, routingKey))
                .verifyComplete();

        verify(defaultValidatedResponseAction).routeResponse(testCmId, jsonNode, routingKey);
    }

    @Test
    void shouldHandleError() {
        String routingKey = X_CM_ID;
        String testCmId = "testCmId";

        when(sender.send(outboundMessageCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(retryableValidatedResponseAction.handleError(new RuntimeException(), testCmId, jsonNode))
                .verifyComplete();

        var outboundMessage = outboundMessageCaptor.getValue().block();
        var headers = outboundMessage.getProperties().getHeaders();
        Assertions.assertEquals(testCmId, headers.get(routingKey));
    }
}
