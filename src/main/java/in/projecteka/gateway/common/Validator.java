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
import static java.util.Optional.of;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
public class Validator {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    BridgeRegistry bridgeRegistry;
    CMRegistry cmRegistry;
    CacheAdapter<String, String> requestIdMappings;

    public Mono<ValidatedRequest> validateRequest(HttpEntity<String> maybeRequest, String routingKey) {
        String clientId = maybeRequest.getHeaders().getFirst(routingKey);
        if (!StringUtils.hasText(clientId)) {
            return error(ClientError.idMissingInHeader(routingKey));
        }
        return getRegistryMapping(bridgeRegistry, cmRegistry, routingKey, clientId)
                .map(registry -> toRequest(maybeRequest, registry))
                .orElseGet(() -> {
                    logger.error("No mapping found for {} : {}", routingKey, clientId);
                    return error(ClientError.mappingNotFoundForId(routingKey));
                });
    }

    private static Mono<ValidatedRequest> toRequest(HttpEntity<String> maybeRequest, YamlRegistryMapping mapping) {
        return Serializer.from(maybeRequest)
                .filter(request -> StringUtils.hasText((String) request.get(REQUEST_ID)))
                .flatMap(request ->
                        from((String) request.get(REQUEST_ID))
                                .map(requestUUID -> just(new ValidatedRequest(mapping, requestUUID, request))))
                .orElseGet(() -> {
                    var errorMessage = format("Empty/Invalid %s found on the payload", REQUEST_ID);
                    logger.error(errorMessage);
                    return error(invalidRequest(errorMessage));
                });
    }

    private static Optional<UUID> from(String requestId) {
        try {
            return of(UUID.fromString(requestId));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    private static Optional<YamlRegistryMapping> getRegistryMapping(BridgeRegistry bridgeRegistry,
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

    public Mono<ValidatedResponse> validateResponse(HttpEntity<String> maybeResponse, String id) {
        String xid = maybeResponse.getHeaders().getFirst(id);
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
        return Serializer.deserializeRequestAsJsonNode(maybeResponse)
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
