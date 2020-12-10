package in.projecteka.gateway.common;

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
import java.util.Map;

import static in.projecteka.gateway.common.Constants.CORRELATION_ID;
import static in.projecteka.gateway.common.Constants.GW_EXCHANGE;
import static in.projecteka.gateway.common.Constants.X_ORIGIN_ID;

@AllArgsConstructor
public class RetryableValidatedRequestAction<T extends ServiceClient>
        implements ValidatedRequestAction {
    private static final Logger logger = LoggerFactory.getLogger(RetryableValidatedRequestAction.class);
    private final Receiver receiver;
    private final Sender sender;
    private final DefaultValidatedRequestAction<T> defaultValidatedRequestAction;
    private final ServiceOptions serviceOptions;
    private final String rabbitMQRoutingKey;
    private final String clientIdRequestHeader;

    @PostConstruct
    public void subscribe() {
        receiver.consumeManualAck(rabbitMQRoutingKey)
                .subscribe(delivery -> processDelivery(delivery).subscribe());
    }

    @PreDestroy
    public void closeConnection() {
        receiver.close();
        sender.close();
    }

    private RetryBackoffSpec retryConfig(AcknowledgableDelivery delivery) {
        return Retry
                .fixedDelay(serviceOptions.getResponseMaxRetryAttempts(), Duration.ofMillis(serviceOptions.getRetryAttemptsDelay()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    logger.info("Exhausted Retries");
                    delivery.nack(false);
                    return new RetryLimitExceededException("Retry limit exceeded for routing the request");
                });
    }

    private Map<String, Object> extractRequestData(TraceableMessage traceableMessage){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.convertValue(traceableMessage.getMessage(), Map.class);
    }

    @Override
    public Mono<Void> routeRequest(String sourceId, String targetId, Map<String, Object> updatedRequest, String routingKey) {
        return defaultValidatedRequestAction.routeRequest(sourceId, targetId, updatedRequest, routingKey);
    }

   // Todo: need to route response back to the caller ( callerDetails (id,response api) )
    @Override
    public Mono<Void> handleError(Throwable throwable, String id, Map<String, Object> map, String sourceId) {
        logger.error("Error in notifying bridge with result; Will push for retry", throwable);
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(map)
                .build();

        return Serializer.from(traceableMessage).map(message -> {
            var headers = new HashMap<String, Object>();
            headers.put(clientIdRequestHeader, id);
            headers.put(X_ORIGIN_ID, sourceId);
            var messageProperties = new AMQP.BasicProperties.Builder().headers(headers).build();
            OutboundMessage outboundMessage = new OutboundMessage(GW_EXCHANGE, rabbitMQRoutingKey, messageProperties, message.getBytes());
            return sender.send(Mono.just(outboundMessage));
        }).orElse(Mono.empty());
    }

    public Mono<Void> processDelivery(AcknowledgableDelivery delivery) {
        TraceableMessage traceableMessage = Serializer.to(delivery.getBody(), TraceableMessage.class);
        var targetId = (LongString) delivery.getProperties().getHeaders().get(clientIdRequestHeader);
        var sourceId = (LongString) delivery.getProperties().getHeaders().get(X_ORIGIN_ID);
        return Mono.just(traceableMessage)
                .map(this::extractRequestData)
                .flatMap((requestData) -> this.routeRequest(sourceId.toString(), targetId.toString(), requestData, clientIdRequestHeader))
                .doOnSuccess(unused -> delivery.ack())
                .doOnError(throwable -> logger.error("Error while processing retryable request", throwable))
                .doFinally(signalType -> MDC.clear())
                .retryWhen(retryConfig(delivery))
                .subscriberContext(ctx -> ctx.put(CORRELATION_ID, traceableMessage.getCorrelationId()));
    }
}
