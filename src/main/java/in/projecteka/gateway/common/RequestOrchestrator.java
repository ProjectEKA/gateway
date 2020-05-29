package in.projecteka.gateway.common;

import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.ServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.common.model.GatewayResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.common.Constants.REQUEST_ID;

@AllArgsConstructor
public class RequestOrchestrator<T extends ServiceClient> {
    private static final Logger logger = LoggerFactory.getLogger(RequestOrchestrator.class);
    CacheAdapter<String, String> requestIdMappings;
    Validator validator;
    T serviceClient;

    public Mono<Void> processRequest(HttpEntity<String> requestEntity, String id) {
        UUID gatewayRequestId = UUID.randomUUID();
        return validator.validateRequest(requestEntity, id)
                .onErrorResume(ClientError.class,
                        clientError -> errorNotify(requestEntity, clientError.getError().getError())
                                .then(Mono.empty()))
                .flatMap(validatedDiscoverRequest -> {
                    Map<String, Object> deserializedRequest = validatedDiscoverRequest.getDeserializedRequest();
                    deserializedRequest.put(REQUEST_ID, gatewayRequestId);
                    return requestIdMappings.put(gatewayRequestId.toString(), validatedDiscoverRequest.getRequesterRequestId())
                            .thenReturn(deserializedRequest)
                            .flatMap(updatedRequest -> serviceClient
                                    .routeRequest(updatedRequest, validatedDiscoverRequest.getConfig().getHost())
                                    .onErrorResume(TimeoutException.class,
                                            throwable -> errorNotify(requestEntity,
                                                    Error.builder()
                                                            .code(ErrorCode.UNKNOWN_ERROR_OCCURRED)
                                                            .message("Timedout When calling bridge").build()))
                                    .onErrorResume(throwable -> errorNotify(requestEntity,
                                            Error.builder()
                                                    .code(ErrorCode.UNKNOWN_ERROR_OCCURRED)
                                                    .message("Error in making call to Bridge").build())));
                });
    }

    Mono<Void> errorNotify(HttpEntity<String> requestEntity, Error error) {
        return Utils.deserializeRequest(requestEntity)
                .map(deserializedRequest -> toErrorResult(error, deserializedRequest))
                .flatMap(errorResult -> serviceClient.notifyError(errorResult));
    }

    private ErrorResult toErrorResult(Error error, Map<String, Object> deserializedRequest) {
        return ErrorResult.builder()
                .requestId(UUID.randomUUID())
                .error(error)
                .resp(GatewayResponse.builder()
                        .requestId(UUID.fromString((String) deserializedRequest.getOrDefault(REQUEST_ID, "")))
                        .build())
                .build();
    }
}
