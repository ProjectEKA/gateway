package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface ValidatedResponseAction {

    default Mono<Void> execute(String routingKey, String clientId, JsonNode updatedRequest) {
        return routeResponse(routingKey, clientId, updatedRequest)
                .onErrorResume(throwable -> handleError(throwable, clientId, updatedRequest));
    }

    Mono<Void> routeResponse(String routingKey, String id, JsonNode updatedRequest);

    Mono<Void> handleError(Throwable throwable, String id, JsonNode jsonNode);
}
