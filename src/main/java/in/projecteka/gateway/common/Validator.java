package in.projecteka.gateway.common;

import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;


@AllArgsConstructor
public class Validator {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    BridgeRegistry bridgeRegistry;
    CMRegistry cmRegistry;
    CacheAdapter<String, String> requestIdMappings;


    public Mono<ValidatedRequest> validateRequest(HttpEntity<String> requestEntity, String id) {
        String xid = requestEntity.getHeaders().getFirst(id);
        if (!StringUtils.hasText(xid)) {
            return Mono.error(ClientError.idMissingInHeader(id));
        }
        Optional<YamlRegistryMapping> config = id.equals(X_HIP_ID)
                ? bridgeRegistry.getConfigFor(xid, ServiceType.HIP)
                : cmRegistry.getConfigFor(xid);
        if (config.isEmpty()) {
            logger.error("No mapping found for {} : {}", id, xid);
            return Mono.error(ClientError.mappingNotFoundForId(id));
        }
        YamlRegistryMapping mapping = config.get();
        return Utils.deserializeRequest(requestEntity)
                .flatMap(deserializedRequest -> {
                    String requestId = (String) deserializedRequest.get(REQUEST_ID);
                    if (!StringUtils.hasText(requestId)) {
                        logger.error("No {} found on the payload", REQUEST_ID);
                        return Mono.empty();
                    }
                    return Mono.just(new ValidatedRequest(mapping, requestId, deserializedRequest));
                });
    }

    public Mono<ValidatedResponse> validateResponse(HttpEntity<String> requestEntity, String id) {
        String xid = requestEntity.getHeaders().getFirst(id);
        if (!StringUtils.hasText(xid)) {
            logger.error("No {} found on Headers", id);
            return Mono.empty();
        }
        Optional<YamlRegistryMapping> config = id.equals(X_HIU_ID)
                ? bridgeRegistry.getConfigFor(xid, ServiceType.HIU)
                : cmRegistry.getConfigFor(xid);
        if (config.isEmpty()) {
            logger.error("No mapping found for {} : {}", id, xid);
            return Mono.empty();
        }
        return Utils.deserializeRequestAsJsonNode(requestEntity)
                .flatMap(jsonNode -> {
                    String respRequestId = jsonNode.path("resp").path(REQUEST_ID).asText();
                    if (respRequestId.isEmpty()) {
                        logger.error("resp.requestId is null or empty");
                        return Mono.empty();
                    }
                    return requestIdMappings.get(respRequestId)
                            .doOnSuccess(callerRequestId -> {
                                if (!StringUtils.hasText(callerRequestId)) {
                                    logger.error("No mapping found for resp.requestId on cache");
                                }
                            })
                            .map(callerRequestId -> new ValidatedResponse(xid, callerRequestId,
                                    jsonNode));
                });
    }
}
