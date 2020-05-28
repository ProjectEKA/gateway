package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ServiceClient;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Optional;

@AllArgsConstructor
public class DefaultValidatedResponseAction<T extends ServiceClient> implements ValidatedResponseAction{
    private static final Logger logger = LoggerFactory.getLogger(DefaultValidatedResponseAction.class);
    T serviceClient;
    CMRegistry cmRegistry;

    @Override
    public Mono<Void> routeResponse(String xCmId, JsonNode updatedRequest) {
        Optional<YamlRegistryMapping> configFor = cmRegistry.getConfigFor(xCmId);
        if (configFor.isEmpty()) {
            logger.error("No mapping found for {}",xCmId);
            return Mono.empty();
        }
        return serviceClient.routeResponse(updatedRequest,configFor.get().getHost());
    }

    @Override
    public Mono<Void> handleError(Throwable throwable, String xCmId, JsonNode jsonNode) {
        //Does it make sense to call the same API back to notify only Error?
        logger.error("Error in notifying CM with result",throwable);
        return Mono.empty();
    }
}
