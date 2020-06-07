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
import java.util.UUID;

import static in.projecteka.gateway.clients.ClientError.invalidRequest;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static java.lang.String.format;

@AllArgsConstructor
public class Validator {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    BridgeRegistry bridgeRegistry;
    CMRegistry cmRegistry;
    CacheAdapter<String, String> requestIdMappings;

    public Mono<ValidatedRequest> validateRequest(HttpEntity<String> requestEntity, String routingKey) {
        String clientId = requestEntity.getHeaders().getFirst(routingKey);
        if (!StringUtils.hasText(clientId)) {
            return Mono.error(ClientError.idMissingInHeader(routingKey));
        }
        return getRegistryMapping(bridgeRegistry, cmRegistry, routingKey, clientId)
                .map(mapping -> Serializer.from(requestEntity)
                        .filter(request -> StringUtils.hasText((String) request.get(REQUEST_ID)))
                        .map(request -> {
                            var requestId = (String) request.get(REQUEST_ID);
                            return to(requestId)
                                    .map(uuid -> Mono.just(new ValidatedRequest(mapping, uuid, request)))
                                    .orElseGet(() -> {
                                        var errorMessage = format("Failed to parse %s: %s found on the payload",
                                                REQUEST_ID,
                                                requestId);
                                        logger.error(errorMessage);
                                        return Mono.error(invalidRequest(errorMessage));
                                    });
                        })
                        .orElseGet(() -> {
                            var errorMessage = format("No %s found on the payload", REQUEST_ID);
                            logger.error(errorMessage);
                            return Mono.error(invalidRequest(errorMessage));
                        }))
                .orElseGet(() -> {
                    logger.error("No mapping found for {} : {}", routingKey, clientId);
                    return Mono.error(ClientError.mappingNotFoundForId(routingKey));
                });
    }

    private Optional<UUID> to(String requestId) {
        try {
            return Optional.of(UUID.fromString(requestId));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Optional<YamlRegistryMapping> getRegistryMapping(BridgeRegistry bridgeRegistry,
                                                             CMRegistry cmRegistry,
                                                             String routingHeaderKey,
                                                             String clientId) {
        if (routingHeaderKey.equals(X_HIP_ID)) {
            return bridgeRegistry.getConfigFor(clientId, ServiceType.HIP);
        } else if (routingHeaderKey.equals(X_HIU_ID)) {
            return bridgeRegistry.getConfigFor(clientId, ServiceType.HIU);
        } else {
            return cmRegistry.getConfigFor(clientId);
        }
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
        return Serializer.deserializeRequestAsJsonNode(requestEntity)
                .flatMap(jsonNode -> {
                    String respRequestId = jsonNode.path("resp").path(REQUEST_ID).asText();
                    if (respRequestId.isEmpty()) {
                        logger.error("resp.requestId is null or empty");
                        // TODO: It's very well an error.
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
