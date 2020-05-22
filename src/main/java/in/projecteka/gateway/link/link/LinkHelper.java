package in.projecteka.gateway.link.link;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.LinkServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.link.discovery.Constants;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.link.discovery.Constants.REQUEST_ID;

@AllArgsConstructor
public class LinkHelper {
    private static final Logger logger = LoggerFactory.getLogger(LinkHelper.class);
    LinkValidator linkValidator;
    CacheAdapter<String, String> requestIdMappings;
    LinkServiceClient linkServiceClient;

    public Mono<Void> doLinkInit(HttpEntity<String> requestEntity) {
        UUID gatewayRequestId = UUID.randomUUID();
        return linkValidator.validateLinkInit(requestEntity)
                .onErrorResume(ClientError.class,
                        error -> linkValidator.errorNotify(requestEntity, Constants.TEMP_CM_ID,
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
                                            throwable -> linkValidator.errorNotify(requestEntity,
                                                    Constants.TEMP_CM_ID,
                                                    Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message(
                                                            "Timed out when calling bridge").build()))
                                    .onErrorResume(throwable -> linkValidator.errorNotify(requestEntity,
                                            Constants.TEMP_CM_ID,
                                            Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message(
                                                    "Error in making call to Bridge").build())));
                });
    }

    public Mono<Void> doOnLinkInit(HttpEntity<String> requestEntity) {
        return linkValidator.validateOnLinkInit(requestEntity)
                .flatMap(validatedLinkInitResponse -> {
                    JsonNode updatedJsonNode = updateRequestId(validatedLinkInitResponse.getDeserializedJsonNode(),
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

    private JsonNode updateRequestId(JsonNode jsonNode, String callerRequestId) {
        ObjectNode mutableNode = (ObjectNode) jsonNode;
        mutableNode.put(REQUEST_ID, UUID.randomUUID().toString());
        ObjectNode respNode = (ObjectNode) mutableNode.get("resp");
        respNode.put(REQUEST_ID, callerRequestId);
        return jsonNode;
    }
}
