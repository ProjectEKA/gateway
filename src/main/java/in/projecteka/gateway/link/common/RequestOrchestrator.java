package in.projecteka.gateway.link.common;

import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.ServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.link.common.model.ErrorResult;
import in.projecteka.gateway.link.common.model.GatewayResponse;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.link.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.link.common.Constants.TEMP_CM_ID;
import static in.projecteka.gateway.link.common.Constants.TRANSACTION_ID;

@AllArgsConstructor
public class RequestOrchestrator<T extends ServiceClient> {
    CacheAdapter<String,String> requestIdMappings;

    Validator validator;

    T serviceClient;
    CMRegistry cmRegistry;

    private static final Logger logger = LoggerFactory.getLogger(RequestOrchestrator.class);

    public Mono<Void> processRequest(HttpEntity<String> requestEntity) {
        UUID gatewayRequestId = UUID.randomUUID();
        return validator.validateRequest(requestEntity)
                .onErrorResume(ClientError.class, clientError -> errorNotify(requestEntity,TEMP_CM_ID,clientError.getError().getError()).then(Mono.empty()))
                .flatMap(validatedDiscoverRequest -> {
                    Map<String, Object> deserializedRequest = validatedDiscoverRequest.getDeserializedRequest();
                    deserializedRequest.put(REQUEST_ID, gatewayRequestId);
                    return requestIdMappings.put(gatewayRequestId.toString(), validatedDiscoverRequest.getCmRequestId())
                            .thenReturn(deserializedRequest)
                            .flatMap(updatedRequest -> serviceClient
                                    .routeRequest(updatedRequest, validatedDiscoverRequest.getHipConfig().getHost())
                                    .onErrorResume(TimeoutException.class,
                                            throwable -> errorNotify(requestEntity, Constants.TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("Timedout When calling bridge").build()))
                                    .onErrorResume(throwable -> errorNotify(requestEntity, Constants.TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("Error in making call to Bridge").build())));
                        });
    }

    Mono<Void> errorNotify(HttpEntity<String> requestEntity, String cmId, Error error) {
        return Utils.deserializeRequest(requestEntity)
                .map(deserializedRequest ->
                        Tuples.of((String) deserializedRequest.getOrDefault(REQUEST_ID, ""),
                                (String) deserializedRequest.getOrDefault(TRANSACTION_ID, "")))
                .filter(tuple -> {
                    String transactionId = tuple.getT2();
                    if (transactionId.isEmpty()) {
                        logger.error("TransactionId is empty");
                    }
                    return !transactionId.isEmpty();
                })
                .map(tuple -> ErrorResult.builder().error(error)
                        .resp(GatewayResponse.builder().requestId(UUID.fromString(tuple.getT1())).build())
                        .requestId(UUID.randomUUID())
                        .transactionId(UUID.fromString(tuple.getT2()))
                        .build()).flatMap(errorResult -> {
                    YamlRegistryMapping cmRegistryMapping = cmRegistry.getConfigFor(cmId).get();//TODO checkback when
                    // cmid is dynamic
                    return serviceClient.notifyError(errorResult, cmRegistryMapping.getHost());
                });
    }
}
