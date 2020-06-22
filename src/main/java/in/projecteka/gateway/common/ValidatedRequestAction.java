package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ValidatedRequestAction {

    default Mono<Void> execute(String clientId, Map<String, Object> updatedRequest) {
        return routeRequest(clientId, updatedRequest)
                .onErrorResume(throwable -> handleError(throwable, clientId, updatedRequest));
    }

    Mono<Void> routeRequest(String id, Map<String,Object> updatedRequest);

    Mono<Void> handleError(Throwable throwable, String id, Map<String, Object> map);
}