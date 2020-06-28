package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ServiceClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class DefaultValidatedResponseAction<T extends ServiceClient> implements ValidatedResponseAction {

    private static final Logger logger = LoggerFactory.getLogger(DefaultValidatedResponseAction.class);

    T serviceClient;

    @Override
    public Mono<Void> routeResponse(String clientId, JsonNode updatedRequest, String routingKey) {
        return serviceClient.routeResponse(updatedRequest, clientId, routingKey);
    }

    @Override
    public Mono<Void> handleError(Throwable throwable, String id, JsonNode jsonNode) {
        //Does it make sense to call the same API back to notify only Error?
        logger.error("Error in notifying host with result", throwable);
        return Mono.empty();
    }
}
