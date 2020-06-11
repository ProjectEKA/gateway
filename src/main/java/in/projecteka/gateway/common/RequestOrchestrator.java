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

    public Mono<Void> handleThis(HttpEntity<String> maybeRequest, String routingKey, String clientId) {
        return validator.validateRequest(maybeRequest, routingKey)
                .doOnSuccess(request -> offloadThis(request, clientId))
                .then();
    }

    private void offloadThis(ValidatedRequest validatedRequest, String clientId) {
        Mono.defer(() -> {
            var gatewayRequestId = UUID.randomUUID();
            var downstreamRequestId = gatewayRequestId.toString();
            var request = validatedRequest.getDeSerializedRequest();
            var upstreamRequestId = validatedRequest.getRequesterRequestId();
            request.put(REQUEST_ID, gatewayRequestId);
            return requestIdMappings.put(downstreamRequestId, upstreamRequestId.toString())
                    .thenReturn(request)
                    .flatMap(updatedRequest ->
                            serviceClient.routeRequest(updatedRequest,
                                    validatedRequest.getConfig().getHost()))
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
                                serviceClient.notifyError(clientId, errorResult).subscribe();
                            });
        }).subscribe();
    }

    private ErrorResult from(Error error, UUID requestId) {
        return ErrorResult.builder()
                .requestId(UUID.randomUUID())
                .error(error)
                .resp(new GatewayResponse(requestId))
                .build();
    }
}
