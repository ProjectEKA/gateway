package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface ValidatedResponseAction {
    default Mono<Void> execute(String x_id, String id, JsonNode updatedRequest) {
        return routeResponse(x_id, id, updatedRequest)
                .onErrorResume(throwable -> handleError(throwable, id, updatedRequest));
    }

    Mono<Void> routeResponse(String x_id, String id, JsonNode updatedRequest);

    Mono<Void> handleError(Throwable throwable, String id, JsonNode jsonNode);
}
