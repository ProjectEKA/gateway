package in.projecteka.gateway.common;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface ValidatedRequestAction {

    default Mono<Void> execute(String clientId, Map<String, Object> updatedRequest, String routingKey) {
        return routeRequest(clientId, updatedRequest, routingKey)
                .onErrorResume(throwable -> handleError(throwable, clientId, updatedRequest));
    }

    Mono<Void> routeRequest(String id, Map<String, Object> updatedRequest, String routingKey);

    Mono<Void> handleError(Throwable throwable, String id, Map<String, Object> map);
}