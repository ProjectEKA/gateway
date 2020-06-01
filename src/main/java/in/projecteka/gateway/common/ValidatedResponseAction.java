package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface ValidatedResponseAction {
    default Mono<Void> execute(String id, JsonNode updatedRequest) {
        return routeResponse(id, updatedRequest)
                .onErrorResume(throwable -> handleError(throwable, id, updatedRequest));
    }

    Mono<Void> routeResponse(String id, JsonNode updatedRequest);

    Mono<Void> handleError(Throwable throwable, String id, JsonNode jsonNode);
}
