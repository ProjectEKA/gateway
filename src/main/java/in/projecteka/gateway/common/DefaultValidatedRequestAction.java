package in.projecteka.gateway.common;

import in.projecteka.gateway.clients.ServiceClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public class DefaultValidatedRequestAction<T extends ServiceClient> implements ValidatedRequestAction {

    private static final Logger logger = LoggerFactory.getLogger(DefaultValidatedResponseAction.class);

    T serviceClient;

    @Override
    public Mono<Void> routeRequest(String id, Map<String, Object> updatedRequest, String routingKey) {
        return serviceClient.routeRequest(updatedRequest, id, routingKey);
    }

    @Override
    public Mono<Void> handleError(Throwable throwable, String id, Map<String, Object> map) {
        logger.error("Error in notifying host with result", throwable);
        return error(throwable);
    }
}
