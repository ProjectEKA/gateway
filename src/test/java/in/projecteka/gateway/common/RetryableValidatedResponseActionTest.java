package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.serviceOptions;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryableValidatedResponseActionTest {
    @Mock
    private AmqpTemplate amqpTemplate;

    @Mock
    private Jackson2JsonMessageConverter converter;

    @Mock
    private JsonNode jsonNode;

    private Message message;

    @Mock
    private DefaultValidatedResponseAction<ServiceClient> defaultValidatedResponseAction;

    private RetryableValidatedResponseAction<ServiceClient> retryableValidatedResponseAction;
    @Captor
    private ArgumentCaptor<MessagePostProcessor> messagePostProcessorArgumentCaptor;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        message = Mockito.mock(Message.class, RETURNS_DEEP_STUBS);
        retryableValidatedResponseAction = Mockito.spy(new RetryableValidatedResponseAction<>(amqpTemplate,
                converter,
                defaultValidatedResponseAction,
                serviceOptions().responseMaxRetryAttempts(5).build(),
                Constants.GW_LINK_QUEUE,
                X_CM_ID));
    }

    @Test
    void shouldRouteResponseAndNotRetryWhenSuccessful() {
        MessageProperties props = new MessageProperties();
        String testCmId = "testCmId";
        var routingKey = X_CM_ID;
        props.setHeader(routingKey, testCmId);
        when(converter.fromMessage(message, ParameterizedTypeReference.forType(JsonNode.class))).thenReturn(jsonNode);
        when(message.getMessageProperties().getHeader(routingKey)).thenReturn(testCmId);
        doReturn(Mono.empty()).when(retryableValidatedResponseAction).routeResponse(testCmId, jsonNode, routingKey);

        retryableValidatedResponseAction.onMessage(message);

        verify(retryableValidatedResponseAction).routeResponse(testCmId, jsonNode, routingKey);
    }

    @Test
    void shouldPushToDLQUponFailureWhenMaxRetryHasNotExceeded() {
        MessageProperties props = new MessageProperties();
        String testCmId = "testCmId";
        var routingKey = X_CM_ID;
        props.setHeader(routingKey, testCmId);
        when(converter.fromMessage(message, ParameterizedTypeReference.forType(JsonNode.class))).thenReturn(jsonNode);
        when(message.getMessageProperties().getHeader(routingKey)).thenReturn(testCmId);
        doReturn(false).when(retryableValidatedResponseAction).hasExceededRetryCount(message);
        doReturn(Mono.error(new RuntimeException()))
                .when(retryableValidatedResponseAction)
                .routeResponse(testCmId, jsonNode, routingKey);

        Assertions.assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> retryableValidatedResponseAction.onMessage(message));

        verify(retryableValidatedResponseAction).routeResponse(testCmId, jsonNode, routingKey);
        verify(retryableValidatedResponseAction).hasExceededRetryCount(message);
    }

    @Test
    void shouldPushToParkingLotUponFailureWhenMaxRetryHasExceeded() {
        MessageProperties props = new MessageProperties();
        String testCmId = "testCmId";
        var routingKey = X_CM_ID;
        props.setHeader(routingKey, testCmId);
        when(converter.fromMessage(message, ParameterizedTypeReference.forType(JsonNode.class))).thenReturn(jsonNode);
        when(message.getMessageProperties().getHeader(routingKey)).thenReturn(testCmId);
        String testRoutingKey = "testRoutingKey";
        when(message.getMessageProperties().getReceivedRoutingKey()).thenReturn(testRoutingKey);
        doNothing().when(amqpTemplate).convertAndSend("gw.parking.exchange", testRoutingKey, message);
        doReturn(true).when(retryableValidatedResponseAction).hasExceededRetryCount(message);
        doReturn(Mono.error(new RuntimeException()))
                .when(retryableValidatedResponseAction)
                .routeResponse(testCmId, jsonNode, routingKey);

        retryableValidatedResponseAction.onMessage(message);

        verify(retryableValidatedResponseAction).routeResponse(testCmId, jsonNode, routingKey);
        verify(retryableValidatedResponseAction).hasExceededRetryCount(message);
        verify(amqpTemplate).convertAndSend("gw.parking.exchange", testRoutingKey, message);
    }

    @Test
    void shouldReturnFalseWhenDeathHeaderCountIsLessThanMaxAttempts() {
        List<Map<String, ?>> xDeathHeaders = new ArrayList<>();
        Map<String, Long> xDeathHeader = new HashMap<>();
        xDeathHeader.put("count", 1L);
        xDeathHeaders.add(xDeathHeader);
        when(message.getMessageProperties().getXDeathHeader()).thenReturn(xDeathHeaders);

        boolean hasExceededRetryCount = retryableValidatedResponseAction.hasExceededRetryCount(message);

        Assertions.assertFalse(hasExceededRetryCount);
    }

    @Test
    void shouldReturnTrueWhenDeathHeaderCountEqualsMaxAttempts() {
        List<Map<String, ?>> xDeathHeaders = new ArrayList<>();
        Map<String, Long> xDeathHeader = new HashMap<>();
        xDeathHeader.put("count", 5L);
        xDeathHeaders.add(xDeathHeader);
        when(message.getMessageProperties().getXDeathHeader()).thenReturn(xDeathHeaders);

        boolean hasExceededRetryCount = retryableValidatedResponseAction.hasExceededRetryCount(message);

        Assertions.assertTrue(hasExceededRetryCount);
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
        String testCmId = "testCmId";
        Map<String, Object> headers = new HashMap<>();
        doNothing().when(amqpTemplate).convertAndSend(eq("gw.dead-letter-exchange"),
                eq(Constants.GW_LINK_QUEUE),
                eq(jsonNode),
                messagePostProcessorArgumentCaptor.capture());
        when(message.getMessageProperties().getHeaders()).thenReturn(headers);

        StepVerifier.create(retryableValidatedResponseAction.handleError(new RuntimeException(), testCmId, jsonNode))
                .verifyComplete();

        MessagePostProcessor messagePostProcessor = messagePostProcessorArgumentCaptor.getValue();
        messagePostProcessor.postProcessMessage(message);
        Assertions.assertEquals(testCmId, headers.get("X-CM-ID"));
    }
}