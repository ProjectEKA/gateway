package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.databind.node.ObjectNode;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.common.cache.CacheAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.link.discovery.Constants.REQUEST_ID;

@Component
public class DiscoveryHelper {
    @Autowired
    CacheAdapter<String,String> requestIdMappings;

    @Autowired
    DiscoveryValidator discoveryValidator;

    @Autowired
    DiscoveryServiceClient discoveryServiceClient;

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryHelper.class);

    Mono<Void> doDiscoverCareContext(HttpEntity<String> requestEntity) {
        UUID gatewayRequestId = UUID.randomUUID();
        return discoveryValidator.validateDiscoverRequest(requestEntity)
                .filter(Tuple2::getT2)
                .flatMap(validatedTuple -> Utils.deserializeRequest(requestEntity)
                        .flatMap(deserializedRequest -> {
                            String cmRequestId = (String) deserializedRequest.get(REQUEST_ID);
                            if (cmRequestId==null || cmRequestId.isEmpty()) {
                                logger.error("No {} found on the payload",REQUEST_ID);
                                return Mono.empty();
                            }
                            deserializedRequest.put(REQUEST_ID, gatewayRequestId);
                            return requestIdMappings.put(gatewayRequestId.toString(), cmRequestId)
                                    .thenReturn(deserializedRequest);
                        })
                        .flatMap(updatedRequest -> discoveryServiceClient
                                .patientFor(updatedRequest, validatedTuple.getT1().getHipConfig().getHost())
                                .onErrorResume(throwable -> (throwable instanceof TimeoutException),
                                        throwable -> discoveryValidator .errorNotify(requestEntity,Constants.TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("Timedout When calling bridge").build()))
                                .onErrorResume(throwable -> discoveryValidator.errorNotify(requestEntity, Constants.TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("Error in making call to Bridge").build()))));
    }

    Mono<Void> doOnDiscoverCareContext(HttpEntity<String> requestEntity) {
        return discoveryValidator.validateDiscoverResponse(requestEntity)
                .filter(Tuple2::getT2)
                .flatMap(validRequest -> Utils.deserializeRequestAsJsonNode(requestEntity)
                        .flatMap(deserializedRequest -> {
                            String gatewayRequestId = deserializedRequest.get("resp").get(REQUEST_ID).asText();
                            if (gatewayRequestId==null || gatewayRequestId.isEmpty()) {
                                logger.error("No {} found on the payload",REQUEST_ID);
                                return Mono.empty();
                            }
                            return requestIdMappings.get(gatewayRequestId)
                                    .doOnSuccess(cmRequestId -> {
                                        if (cmRequestId == null || cmRequestId.isEmpty()) {
                                            logger.error("No cmRequestId mapping found for {}", gatewayRequestId);
                                        }
                                    })
                                    .map(cmRequestId -> {
                                        ObjectNode mutableNode = (ObjectNode) deserializedRequest;
                                        mutableNode.put(REQUEST_ID, UUID.randomUUID().toString());
                                        ObjectNode respNode = (ObjectNode) mutableNode.get("resp");
                                        respNode.put(REQUEST_ID, cmRequestId);
                                        return deserializedRequest;
                                    });
                        })
                        .flatMap(updatedRequest -> discoveryServiceClient
                                .patientDiscoveryResultNotify(updatedRequest,validRequest.getT1().getCmConfig().getHost())
                                .onErrorResume(throwable -> {
                                    //Does it make sense to call the same API back to notify only Error?
                                    logger.error("Error in notifying CM with result",throwable);
                                    return Mono.empty();
                                })));
    }
}
