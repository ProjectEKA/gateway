package in.projecteka.gateway.link.common;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface ValidatedResponseAction {
    default Mono<Void> execute(String xCmId, JsonNode updatedRequest) {
        return routeResponse(xCmId, updatedRequest)
                .onErrorResume(throwable -> handleError(throwable, xCmId,updatedRequest));
    }

    Mono<Void> routeResponse(String xCmId, JsonNode updatedRequest);

    Mono<? extends Void> handleError(Throwable throwable, String xCmId, JsonNode jsonNode);
}
