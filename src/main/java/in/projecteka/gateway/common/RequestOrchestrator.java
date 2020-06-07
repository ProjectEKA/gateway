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
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.clients.model.Error.unKnownError;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;

@AllArgsConstructor
public class RequestOrchestrator<T extends ServiceClient> {
    private static final Logger logger = LoggerFactory.getLogger(RequestOrchestrator.class);
    CacheAdapter<String, String> requestIdMappings;
    Validator validator;
    T serviceClient;

    public Mono<Void> processRequest(HttpEntity<String> requestEntity, String targetSystemHeaderKey, String clientId) {
        return validator.validateRequest(requestEntity, targetSystemHeaderKey)
                .doOnSuccess(validatedDiscoverRequest -> Mono.defer(() -> {
                    var gatewayRequestId = UUID.randomUUID();
                    var downstreamRequestId = gatewayRequestId.toString();
                    var request = validatedDiscoverRequest.getDeSerializedRequest();
                    var upstreamRequestId = validatedDiscoverRequest.getRequesterRequestId();
                    request.put(REQUEST_ID, gatewayRequestId);
                    return requestIdMappings.put(downstreamRequestId, upstreamRequestId.toString())
                            .thenReturn(request)
                            .flatMap(updatedRequest ->
                                    serviceClient.routeRequest(updatedRequest,
                                            validatedDiscoverRequest.getConfig().getHost()))
                            .onErrorMap(ClientError.class,
                                    clientError -> {
                                        logger.error(clientError.getMessage(), clientError);
                                        return toErrorResult(clientError.getError().getError(), upstreamRequestId);
                                    })
                            .onErrorMap(TimeoutException.class,
                                    timeout -> {
                                        logger.error(timeout.getMessage(), timeout);
                                        return toErrorResult(unKnownError("Timed out When calling target system"),
                                                upstreamRequestId);
                                    })
                            .onErrorMap(throwable -> throwable.getClass() != ErrorResult.class,
                                    throwable -> {
                                        logger.error(throwable.getMessage(), throwable);
                                        return toErrorResult(unKnownError("Error in making call to target system"),
                                                upstreamRequestId);
                                    })
                            .doOnError(ErrorResult.class,
                                    errorResult -> {
                                        logger.error("Notifying caller about the failure", errorResult);
                                        serviceClient.notifyError(clientId, errorResult).subscribe();
                                    });
                }).subscribe())
                .then();
    }

    private ErrorResult toErrorResult(Error error, UUID requestId) {
        return ErrorResult.builder()
                .requestId(UUID.randomUUID())
                .error(error)
                .resp(new GatewayResponse(requestId))
                .build();
    }
}
