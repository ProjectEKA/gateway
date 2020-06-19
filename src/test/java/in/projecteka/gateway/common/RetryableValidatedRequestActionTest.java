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

import static in.projecteka.gateway.testcommon.TestBuilders.serviceOptions;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryableValidatedRequestActionTest {
    @Mock
    private AmqpTemplate amqpTemplate;

    @Mock
    private Jackson2JsonMessageConverter converter;

    @Mock
    private JsonNode jsonNode;

    private Message message;

    @Mock
    private Map<String, Object> map;

    @Mock
    private DefaultValidatedRequestAction<ServiceClient> defaultValidatedRequestAction;

    private RetryableValidatedRequestAction<ServiceClient> retryableValidatedRequestAction;
    @Captor
    private ArgumentCaptor<MessagePostProcessor> messagePostProcessorArgumentCaptor;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        message = Mockito.mock(Message.class, RETURNS_DEEP_STUBS);
        retryableValidatedRequestAction = Mockito.spy(new RetryableValidatedRequestAction<>(amqpTemplate,
                converter,
                defaultValidatedRequestAction,
                serviceOptions().responseMaxRetryAttempts(5).build(),
                Constants.GW_DATAFLOW_QUEUE,
                "X-HIP-ID"));
    }

    @Test
    void shouldRouteResponseAndNotRetryWhenSuccessful() {
        MessageProperties props = new MessageProperties();
        String testCmId = "testHipId";
        props.setHeader("X-HIP-ID", testCmId);
        when(converter.fromMessage(message, ParameterizedTypeReference.forType(Map.class))).thenReturn(map);
        when(message.getMessageProperties().getHeader("X-HIP-ID")).thenReturn(testCmId);
        doReturn(Mono.empty()).when(retryableValidatedRequestAction).routeRequest(testCmId, map);

        retryableValidatedRequestAction.onMessage(message);

        verify(retryableValidatedRequestAction).routeRequest(testCmId, map);
    }

    @Test
    void shouldPushToDLQUponFailureWhenMaxRetryHasNotExceeded() {
        MessageProperties props = new MessageProperties();
        String testHipId = "testHipId";
        props.setHeader("X-HIP-ID", testHipId);
        when(converter.fromMessage(message, ParameterizedTypeReference.forType(Map.class))).thenReturn(map);
        when(message.getMessageProperties().getHeader("X-HIP-ID")).thenReturn(testHipId);
        doReturn(false).when(retryableValidatedRequestAction).hasExceededRetryCount(message);
        doReturn(Mono.error(new RuntimeException()))
                .when(retryableValidatedRequestAction)
                .routeRequest(testHipId, map);

        Assertions.assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> retryableValidatedRequestAction.onMessage(message));

        verify(retryableValidatedRequestAction).routeRequest(testHipId, map);
        verify(retryableValidatedRequestAction).hasExceededRetryCount(message);
    }

    @Test
    void shouldPushToParkingLotUponFailureWhenMaxRetryHasExceeded() {
        MessageProperties props = new MessageProperties();
        String testHipId = "testHipId";
        props.setHeader("X-HIP-ID", testHipId);
        when(converter.fromMessage(message, ParameterizedTypeReference.forType(Map.class))).thenReturn(map);
        when(message.getMessageProperties().getHeader("X-HIP-ID")).thenReturn(testHipId);
        String testRoutingKey = "testRoutingKey";
        when(message.getMessageProperties().getReceivedRoutingKey()).thenReturn(testRoutingKey);
        doNothing().when(amqpTemplate).convertAndSend("gw.parking.exchange", testRoutingKey, message);
        doReturn(true).when(retryableValidatedRequestAction).hasExceededRetryCount(message);
        doReturn(Mono.error(new RuntimeException()))
                .when(retryableValidatedRequestAction)
                .routeRequest(testHipId, map);

        retryableValidatedRequestAction.onMessage(message);

        verify(retryableValidatedRequestAction).routeRequest(testHipId, map);
        verify(retryableValidatedRequestAction).hasExceededRetryCount(message);
        verify(amqpTemplate).convertAndSend("gw.parking.exchange", testRoutingKey, message);
    }

    @Test
    void shouldReturnFalseWhenDeathHeaderCountIsLessThanMaxAttempts() {
        List<Map<String, ?>> xDeathHeaders = new ArrayList<>();
        Map<String, Long> xDeathHeader = new HashMap<>();
        xDeathHeader.put("count", 1L);
        xDeathHeaders.add(xDeathHeader);
        when(message.getMessageProperties().getXDeathHeader()).thenReturn(xDeathHeaders);

        boolean hasExceededRetryCount = retryableValidatedRequestAction.hasExceededRetryCount(message);

        Assertions.assertFalse(hasExceededRetryCount);
    }

    @Test
    void shouldReturnTrueWhenDeathHeaderCountEqualsMaxAttempts() {
        List<Map<String, ?>> xDeathHeaders = new ArrayList<>();
        Map<String, Long> xDeathHeader = new HashMap<>();
        xDeathHeader.put("count", 5L);
        xDeathHeaders.add(xDeathHeader);
        when(message.getMessageProperties().getXDeathHeader()).thenReturn(xDeathHeaders);

        boolean hasExceededRetryCount = retryableValidatedRequestAction.hasExceededRetryCount(message);

        Assertions.assertTrue(hasExceededRetryCount);
    }

    @Test
    void shouldRouteResponse() {
        var testHipId = string();
        when(defaultValidatedRequestAction.routeRequest(testHipId, map)).thenReturn(Mono.empty());

        StepVerifier.create(retryableValidatedRequestAction.routeRequest(testHipId, map))
                .verifyComplete();

        verify(defaultValidatedRequestAction).routeRequest(testHipId, map);
    }

    @Test
    void shouldHandleError() {
        String testHipId = "testHipId";
        Map<String, Object> headers = new HashMap<>();
        headers.put("X-HIP-ID",testHipId);
        doNothing().when(amqpTemplate).convertAndSend(eq("gw.dead-letter-exchange"),
                eq(Constants.GW_DATAFLOW_QUEUE),
                eq(map),
                messagePostProcessorArgumentCaptor.capture());
        when(message.getMessageProperties().getHeaders()).thenReturn(headers);

        StepVerifier.create(retryableValidatedRequestAction.handleError(new RuntimeException(), testHipId, map))
                .verifyComplete();

        MessagePostProcessor messagePostProcessor = messagePostProcessorArgumentCaptor.getValue();
        messagePostProcessor.postProcessMessage(message);
        Assertions.assertEquals(testHipId, headers.get("X-HIP-ID"));
    }
}