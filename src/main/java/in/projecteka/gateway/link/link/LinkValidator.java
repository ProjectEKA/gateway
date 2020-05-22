package in.projecteka.gateway.link.link;

import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.link.discovery.Utils;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static in.projecteka.gateway.link.discovery.Constants.REQUEST_ID;
import static in.projecteka.gateway.link.discovery.Constants.X_CM_ID;

@AllArgsConstructor
public class LinkValidator {
    CMRegistry cmRegistry;
    CacheAdapter<String,String> requestIdMappings;

    private static final Logger logger = LoggerFactory.getLogger(LinkValidator.class);

    Mono<Void> validateLinkInit(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }

    Mono<ValidatedLinkInitResponse> validateOnLinkInit(HttpEntity<String> requestEntity) {
        List<String> xCmIds = requestEntity.getHeaders().get(X_CM_ID);
        if (xCmIds==null || xCmIds.isEmpty()) {
            logger.error("No X-CM-ID found on Headers");
            return Mono.empty();
        }
        String xCmId = xCmIds.get(0);
        Optional<YamlRegistryMapping> cmConfig = cmRegistry.getConfigFor(xCmId);
        if (cmConfig.isEmpty()) {
            logger.error("No mapping found for X-CM-ID : {}",xCmId);
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
                            .map(callerRequestId -> new ValidatedLinkInitResponse(cmConfig.get(), callerRequestId,jsonNode));
                });
    }
}
