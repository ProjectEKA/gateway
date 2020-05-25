package in.projecteka.gateway.link.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class RetryableValidatedResponseAction<T extends ServiceClient> implements MessageListener,ValidatedResponseAction {
    private static final Logger logger = LoggerFactory.getLogger(RetryableValidatedResponseAction.class);
    private final AmqpTemplate amqpTemplate;
    private final Jackson2JsonMessageConverter converter;
    private final DefaultValidatedResponseAction<T> defaultValidatedResponseAction;

    public RetryableValidatedResponseAction(AmqpTemplate amqpTemplate, Jackson2JsonMessageConverter converter, DefaultValidatedResponseAction<T> defaultValidatedResponseAction) {
        this.amqpTemplate = amqpTemplate;
        this.converter = converter;
        this.defaultValidatedResponseAction = defaultValidatedResponseAction;
    }

    @Override
    public void onMessage(Message message) {
        if (hasExceededRetryCount(message)) {
            logger.error("Exceeded retry attempts; parking the message");
            amqpTemplate.convertAndSend("gw.parking.exchange",message.getMessageProperties().getReceivedRoutingKey(),message);
            return;
        }
        JsonNode jsonNode = (JsonNode) converter.fromMessage(message, ParameterizedTypeReference.forType(JsonNode.class));
        String xCmId = message.getMessageProperties().getHeader("X-CM-ID");
        try {
            routeResponse(xCmId, jsonNode).block();
        } catch (Exception e) {
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    private boolean hasExceededRetryCount(Message in) {
        List<Map<String, ?>> xDeathHeader = in.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            Long count = (Long) xDeathHeader.get(0).get("count");
            logger.info("[GW] Number of attempts {}",count);
            return count >= 3; //TODO prop
        }
        return false;
    }

    @Override
    public Mono<Void> routeResponse(String xCmId, JsonNode updatedRequest) {
        return defaultValidatedResponseAction.routeResponse(xCmId,updatedRequest);
    }

    @Override
    public Mono<? extends Void> handleError(Throwable throwable, String xCmId, JsonNode jsonNode) {
        logger.error("Error in notifying CM with result; pushing to DLQ for retry",throwable);
        MessagePostProcessor messagePostProcessor = message -> {
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            headers.put("X-CM-ID",xCmId);
            return message;
        };
        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend("gw.dead-letter-exchange", "gw.link", jsonNode, messagePostProcessor);//TODO rename queue
            monoSink.success();
        });
    }
}
