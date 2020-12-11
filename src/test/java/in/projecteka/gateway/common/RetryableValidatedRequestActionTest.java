package in.projecteka.gateway.common;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.common.Constants.GW_DATAFLOW_QUEUE;
import static in.projecteka.gateway.common.Constants.X_ORIGIN_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.serviceOptions;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

class RetryableValidatedRequestActionTest {

    @Mock
    private Receiver receiver;

    @Mock
    private Sender sender;

    @Mock
    private Map<String, Object> map;

    @Mock
    private DefaultValidatedRequestAction<ServiceClient> defaultValidatedRequestAction;

    private RetryableValidatedRequestAction<ServiceClient> retryableValidatedRequestAction;

    private AcknowledgableDelivery acknowledgableDelivery;

    @Captor
    private ArgumentCaptor<Mono<OutboundMessage>> outboundMessageCaptor;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        map = new HashMap<>(); //Sample request body;
        acknowledgableDelivery = Mockito.mock(AcknowledgableDelivery.class, RETURNS_DEEP_STUBS);
        when(receiver.consumeManualAck(GW_DATAFLOW_QUEUE)).thenReturn(Flux.just(acknowledgableDelivery));
        retryableValidatedRequestAction = Mockito.spy(new RetryableValidatedRequestAction<>(receiver,
                sender,
                defaultValidatedRequestAction,
                serviceOptions().responseMaxRetryAttempts(5).retryAttemptsDelay(100).build(),
                Constants.GW_DATAFLOW_QUEUE,
                "X-HIP-ID"));
    }

    @Test
    void shouldRouteResponseAndNotRetryWhenSuccessful() {
        String routingKey = "X-HIP-ID";
        LongString testHipId = LongStringHelper.asLongString("testHipId");
        LongString sourceId = LongStringHelper.asLongString("sourceId");

        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(UUID.randomUUID().toString())
                .message(map).build();

        when(acknowledgableDelivery.getProperties().getHeaders().get(routingKey)).thenReturn(testHipId);
        when(acknowledgableDelivery.getProperties().getHeaders().get(X_ORIGIN_ID)).thenReturn(sourceId);
        when(acknowledgableDelivery.getBody()).thenReturn(Serializer.from(traceableMessage).get().getBytes());

        doReturn(Mono.empty()).when(retryableValidatedRequestAction).routeRequest(sourceId.toString(), testHipId.toString(), map, routingKey);

        retryableValidatedRequestAction.subscribe();

        verify(retryableValidatedRequestAction).routeRequest(sourceId.toString(), testHipId.toString(), map, routingKey);
    }


    @Test
    void shouldRetryIfFailsToRouteRequest() {
        String routingKey = "X-HIP-ID";
        LongString testHipId = LongStringHelper.asLongString("testHipId");
        LongString sourceId = LongStringHelper.asLongString("sourceId");

        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(UUID.randomUUID().toString())
                .message(map).build();

        when(acknowledgableDelivery.getProperties().getHeaders().get(routingKey)).thenReturn(testHipId);
        when(acknowledgableDelivery.getProperties().getHeaders().get(X_ORIGIN_ID)).thenReturn(sourceId);
        when(acknowledgableDelivery.getBody()).thenReturn(Serializer.from(traceableMessage).get().getBytes());

        doReturn(error(mappingNotFoundForId(testHipId.toString())), empty())
                .when(retryableValidatedRequestAction)
                .routeRequest(sourceId.toString(), testHipId.toString(), map, routingKey);

        StepVerifier.create(retryableValidatedRequestAction.processDelivery(acknowledgableDelivery))
                .verifyComplete();

        verify(retryableValidatedRequestAction, times(2)).routeRequest(sourceId.toString(), testHipId.toString(), map, routingKey);
    }

    @Test
    void shouldThrowExceptionWhenRetryLimitExceeds() {
        String routingKey = "X-HIP-ID";
        LongString testHipId = LongStringHelper.asLongString("testHipId");
        LongString sourceId = LongStringHelper.asLongString("sourceId");

        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(UUID.randomUUID().toString())
                .message(map).build();

        when(acknowledgableDelivery.getProperties().getHeaders().get(routingKey)).thenReturn(testHipId);
        when(acknowledgableDelivery.getProperties().getHeaders().get(X_ORIGIN_ID)).thenReturn(sourceId);
        when(acknowledgableDelivery.getBody()).thenReturn(Serializer.from(traceableMessage).get().getBytes());

        doReturn(error(mappingNotFoundForId(testHipId.toString())))
                .when(retryableValidatedRequestAction)
                .routeRequest(sourceId.toString(), testHipId.toString(), map, routingKey);

        StepVerifier.create(retryableValidatedRequestAction.processDelivery(acknowledgableDelivery))
                .expectError(RetryLimitExceededException.class)
                .verify();

        verify(retryableValidatedRequestAction, times(6)).routeRequest(sourceId.toString(), testHipId.toString(), map, routingKey);
    }

    @Test
    void shouldRouteResponse() {
        var testHipId = string();
        var sourceId = string();
        var routingKey = string();

        when(defaultValidatedRequestAction.routeRequest(sourceId, testHipId, map, routingKey)).thenReturn(Mono.empty());

        StepVerifier.create(retryableValidatedRequestAction.routeRequest(sourceId, testHipId, map, routingKey))
                .verifyComplete();

        verify(defaultValidatedRequestAction).routeRequest(sourceId, testHipId, map, routingKey);
    }

    @Test
    void shouldHandleError() {
        String routingKey = "X-HIP-ID";
        String testHipId = "testHipId";
        var sourceId = string();

        when(sender.send(outboundMessageCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(retryableValidatedRequestAction.handleError(new RuntimeException(), testHipId, map, sourceId))
                .verifyComplete();

        var outboundMessage = outboundMessageCaptor.getValue().block();
        var headers = outboundMessage.getProperties().getHeaders();
        Assertions.assertEquals(testHipId, headers.get(routingKey));
        Assertions.assertEquals(sourceId, headers.get(X_ORIGIN_ID));
    }
}
