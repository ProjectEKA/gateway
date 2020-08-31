package in.projecteka.gateway.common;

import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.ServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.common.model.GatewayResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.clients.model.Error.unKnownError;
import static in.projecteka.gateway.common.Constants.CORRELATION_ID;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.TIMESTAMP;
import static in.projecteka.gateway.common.Constants.nameMap;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@AllArgsConstructor
public class RequestOrchestrator<T extends ServiceClient> {
    private static final Logger logger = LoggerFactory.getLogger(RequestOrchestrator.class);
    CacheAdapter<String, String> requestIdMappings;
    RedundantRequestValidator redundantRequestValidator;
    Validator validator;
    T serviceClient;
    ValidatedRequestAction requestAction;

    public Mono<Void> handleThis(HttpEntity<String> maybeRequest,
                                 String targetRoutingKey,
                                 String sourceRoutingKey,
                                 String clientId) {
        StringBuilder apiCalled = new StringBuilder("");
        return Mono.subscriberContext()
                .flatMap(context -> {
                    apiCalled.append((String) context.get("apiCalled"));
                    return validator.validateRequest(maybeRequest, targetRoutingKey);
                }).doOnSuccess(request -> offloadThis(request,
                        targetRoutingKey,
                        sourceRoutingKey,
                        clientId,
                        apiCalled.toString()))
                .then();
    }

    private void offloadThis(ValidatedRequest validatedRequest,
                             String targetRoutingKey,
                             String sourceRoutingKey,
                             String clientId,
                             String apiCalled) {
        Mono.defer(() -> {
            var gatewayRequestId = UUID.randomUUID();
            var downstreamRequestId = gatewayRequestId.toString();
            var request = validatedRequest.getDeSerializedRequest();
            var upstreamRequestId = validatedRequest.getRequesterRequestId();
            request.put(REQUEST_ID, gatewayRequestId);

            logger.info("Received a request {} {} {} {} {} {}", keyValue("requestId", upstreamRequestId)
                    , keyValue("source", nameMap.get(sourceRoutingKey))
                    , keyValue("sourceId", clientId)
                    , keyValue("apiCalled", apiCalled)
                    , keyValue("target", nameMap.get(targetRoutingKey))
                    , keyValue("targetId", validatedRequest.getClientId()));

            return requestIdMappings.put(downstreamRequestId, upstreamRequestId.toString())
                    .then(redundantRequestValidator.put(upstreamRequestId.toString(), request.get(TIMESTAMP).toString()))
                    .thenReturn(request)
                    .flatMap(updatedRequest -> {
                        logger.info("About to call a target {} {}", keyValue("requestId", upstreamRequestId)
                                , keyValue("gatewayId", gatewayRequestId));
                        return requestAction.execute(validatedRequest.getClientId(), updatedRequest, targetRoutingKey);
                    })
                    .onErrorMap(ClientError.class,
                            clientError -> {
                                logger.error(clientError.getMessage(), clientError);
                                return from(clientError.getError().getError(), upstreamRequestId);
                            })
                    .onErrorMap(TimeoutException.class,
                            timeout -> {
                                logger.error(timeout.getMessage(), timeout);
                                return from(unKnownError("Timed out When calling target system"),
                                        upstreamRequestId);
                            })
                    .onErrorMap(throwable -> throwable.getClass() != ErrorResult.class,
                            throwable -> {
                                logger.error(throwable.getMessage(), throwable);
                                return from(unKnownError("Error in making call to target system"),
                                        upstreamRequestId);
                            })
                    .doOnError(ErrorResult.class,
                            errorResult -> {
                                logger.error("Notifying caller about the failure", errorResult);
                                serviceClient.notifyError(clientId, sourceRoutingKey, errorResult).subscribe();
                            });
        }).subscriberContext(ctx -> ctx.put(Constants.CORRELATION_ID, MDC.get(CORRELATION_ID))).subscribe();
    }

    private ErrorResult from(Error error, UUID requestId) {
        return ErrorResult.builder()
                .requestId(UUID.randomUUID())
                .error(error)
                .resp(new GatewayResponse(requestId))
                .build();
    }
}
