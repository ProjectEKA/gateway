package in.projecteka.gateway.link.link;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.LinkServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.link.common.Constants;
import in.projecteka.gateway.link.common.Utils;
import in.projecteka.gateway.link.common.Validator;
import in.projecteka.gateway.link.common.model.GatewayResponse;
import in.projecteka.gateway.link.common.model.ErrorResult;
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
import static in.projecteka.gateway.link.common.Constants.TRANSACTION_ID;

@AllArgsConstructor
public class LinkHelper {
    private static final Logger logger = LoggerFactory.getLogger(LinkHelper.class);
    Validator validator;
    CacheAdapter<String, String> requestIdMappings;
    LinkServiceClient linkServiceClient;
    CMRegistry cmRegistry;

    public Mono<Void> doLinkInit(HttpEntity<String> requestEntity) {
        UUID gatewayRequestId = UUID.randomUUID();
        return validator.validateRequest(requestEntity)
                .onErrorResume(ClientError.class,
                        error -> errorNotify(requestEntity, Constants.TEMP_CM_ID,
                                error.getError().getError())
                                .then(Mono.empty()))
                .flatMap(validatedLinkInitRequest -> {
                    Map<String, Object> deserializedRequest = validatedLinkInitRequest.getDeserializedRequest();
                    deserializedRequest.put(REQUEST_ID, gatewayRequestId);
                    return requestIdMappings.put(gatewayRequestId.toString(), validatedLinkInitRequest.getCmRequestId())
                            .thenReturn(deserializedRequest)
                            .flatMap(updatedRequest -> linkServiceClient
                                    .linkInit(updatedRequest, validatedLinkInitRequest.getHipConfig().getHost())
                                    .onErrorResume(throwable -> (throwable instanceof TimeoutException),
                                            throwable -> errorNotify(requestEntity,
                                                    Constants.TEMP_CM_ID,
                                                    Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message(
                                                            "Timed out when calling bridge").build()))
                                    .onErrorResume(throwable -> errorNotify(requestEntity,
                                            Constants.TEMP_CM_ID,
                                            Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message(
                                                    "Error in making call to Bridge").build())));
                });
    }

    public Mono<Void> doOnLinkInit(HttpEntity<String> requestEntity) {
        return validator.validateResponse(requestEntity)
                .flatMap(validatedLinkInitResponse -> {
                    JsonNode updatedJsonNode = Utils.updateRequestId(validatedLinkInitResponse.getDeserializedJsonNode(),
                            validatedLinkInitResponse.getCallerRequestId());
                    return linkServiceClient.
                            linkOnInitResultNotify(updatedJsonNode,
                                    validatedLinkInitResponse.getCmConfig().getHost())
                            .onErrorResume(throwable -> {
                                logger.error("Error in notifying CM with result", throwable);
                                return Mono.empty();
                            });
                });
    }
    public Mono<Void> errorNotify(HttpEntity<String> requestEntity, String cmId, Error error) {
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
                    return linkServiceClient.linkInitErrorResultNotify(errorResult, cmRegistryMapping.getHost());
                });
    }
}
