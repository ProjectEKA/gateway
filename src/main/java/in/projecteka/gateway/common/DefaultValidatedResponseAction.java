package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ClientRegistryClient;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static in.projecteka.gateway.common.Constants.X_HIU_ID;

@AllArgsConstructor
public class DefaultValidatedResponseAction<T extends ClientRegistryClient.ServiceClient> implements ValidatedResponseAction{
    private static final Logger logger = LoggerFactory.getLogger(DefaultValidatedResponseAction.class);
    T serviceClient;
    CMRegistry cmRegistry;
    BridgeRegistry bridgeRegistry;

    @Override
    public Mono<Void> routeResponse(String routingKey, String clientId, JsonNode updatedRequest) {
        Optional<YamlRegistryMapping> configFor = routingKey.equals(X_HIU_ID)
                ? bridgeRegistry.getConfigFor(clientId, ServiceType.HIU)
                : cmRegistry.getConfigFor(clientId);
        if (configFor.isEmpty()) {
            logger.error("No mapping found for {}", clientId);
            return Mono.empty();
        }
        return serviceClient.routeResponse(updatedRequest,configFor.get().getHost());
    }

    @Override
    public Mono<Void> handleError(Throwable throwable, String id, JsonNode jsonNode) {
        //Does it make sense to call the same API back to notify only Error?
        logger.error("Error in notifying host with result",throwable);
        return Mono.empty();
    }
}
