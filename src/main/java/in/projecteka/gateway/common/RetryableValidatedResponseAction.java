package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.LongString;
import in.projecteka.gateway.clients.ServiceClient;
import in.projecteka.gateway.common.cache.ServiceOptions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;

import static in.projecteka.gateway.common.Constants.CORRELATION_ID;
import static in.projecteka.gateway.common.Constants.GW_EXCHANGE;

@AllArgsConstructor
public class RetryableValidatedResponseAction<T extends ServiceClient> implements ValidatedResponseAction {
    private static final Logger logger = LoggerFactory.getLogger(RetryableValidatedResponseAction.class);

    private Receiver receiver;
    private Sender sender;
    private final DefaultValidatedResponseAction<T> defaultValidatedResponseAction;
    private final ServiceOptions serviceOptions;
    private final String rabbitMQRoutingKey;
    private final String clientIdRequestHeader;

    @PostConstruct
    public void subscribe() {
        receiver.consumeManualAck(rabbitMQRoutingKey)
                .subscribe(delivery -> {
                    TraceableMessage traceableMessage = Serializer.to(delivery.getBody(), TraceableMessage.class);
                    var xCmId = (LongString) delivery.getProperties().getHeaders().get(clientIdRequestHeader);
                    Mono.just(traceableMessage)
                            .map(this::extractRequestData)
                            .flatMap((requestData) -> this.routeResponse(xCmId.toString(), requestData, clientIdRequestHeader))
                            .doOnSuccess(unused -> delivery.ack())
                            .doOnError(throwable -> logger.error("Error while processing retryable response", throwable))
                            .doFinally(signalType -> MDC.clear())
                            .retryWhen(retryConfig(delivery))
                            .subscriberContext(ctx -> ctx.put(CORRELATION_ID, traceableMessage.getCorrelationId()))
                            .subscribe();
                });
    }

    @PreDestroy
    public void closeConnection() {
        receiver.close();
        sender.close();
    }

    private RetryBackoffSpec retryConfig(AcknowledgableDelivery delivery) {
        return Retry
                .fixedDelay(serviceOptions.getResponseMaxRetryAttempts(), Duration.ofMillis(1000))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    logger.info("Exhausted Retries");
                    delivery.nack(false);
                    return new Exception("Failed to route response even after retries");
                });
    }

    private JsonNode extractRequestData(TraceableMessage traceableMessage){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.convertValue(traceableMessage.getMessage(), JsonNode.class);
    }

    @Override
    public Mono<Void> routeResponse(String clientId, JsonNode updatedRequest, String routingKey) {
        return defaultValidatedResponseAction.routeResponse(clientId, updatedRequest, routingKey);
    }

    @Override
    public Mono<Void> handleError(Throwable throwable, String xCmId, JsonNode jsonNode) {
        logger.error("Error in notifying CM with result; pushing to DLQ for retry", throwable);
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(jsonNode)
                .build();

        return Serializer.from(traceableMessage).map(message -> {
            var headers = new HashMap<String, Object>();
            headers.put(clientIdRequestHeader, xCmId);
            var messageProperties = new AMQP.BasicProperties.Builder().headers(headers).build();
            OutboundMessage outboundMessage = new OutboundMessage(GW_EXCHANGE, rabbitMQRoutingKey, messageProperties, message.getBytes());
            return sender.send(Mono.just(outboundMessage));
        }).orElse(Mono.empty());
    }
}
