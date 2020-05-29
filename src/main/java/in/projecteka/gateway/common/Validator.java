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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;

@AllArgsConstructor
public class Validator {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    BridgeRegistry bridgeRegistry;
    CMRegistry cmRegistry;
    CacheAdapter<String, String> requestIdMappings;


    public Mono<ValidatedRequest> validateRequest(HttpEntity<String> requestEntity) {
        List<String> xHipIds = requestEntity.getHeaders().get(X_HIP_ID);
        if (xHipIds == null || xHipIds.isEmpty()) {
            return Mono.error(ClientError.hipIdMissing());
        }
        String xHipId = xHipIds.get(0);
        Optional<YamlRegistryMapping> hipConfig = bridgeRegistry.getConfigFor(xHipId, ServiceType.HIP);
        if (hipConfig.isEmpty()) {
            return Mono.error(ClientError.mappingNotFoundForHipId());
        }
        return Utils.deserializeRequest(requestEntity)
                .flatMap(deserializedRequest -> {
                    String cmRequestId = (String) deserializedRequest.get(REQUEST_ID);
                    if (cmRequestId == null || cmRequestId.isEmpty()) {
                        logger.error("No {} found on the payload", REQUEST_ID);
                        return Mono.empty();
                    }
                    return Mono.just(new ValidatedRequest(hipConfig.get(), cmRequestId, deserializedRequest));
                });
    }

    public Mono<ValidatedResponse> validateResponse(HttpEntity<String> requestEntity) {
        List<String> xCmIds = requestEntity.getHeaders().get(X_CM_ID);
        if (xCmIds == null || xCmIds.isEmpty()) {
            logger.error("No X-CM-ID found on Headers");
            return Mono.empty();
        }
        String xCmId = xCmIds.get(0);
        Optional<YamlRegistryMapping> cmConfig = cmRegistry.getConfigFor(xCmId);
        if (cmConfig.isEmpty()) {
            logger.error("No mapping found for X-CM-ID : {}", xCmId);
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
                                if (callerRequestId == null || callerRequestId.isEmpty()) {
                                    logger.error("No mapping found for resp.requestId on cache");
                                }
                            })
                            .map(callerRequestId -> new ValidatedResponse(xCmId, callerRequestId,
                                    jsonNode));
                });
    }
}
