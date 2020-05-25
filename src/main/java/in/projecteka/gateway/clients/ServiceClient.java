package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.link.common.model.ErrorResult;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ServiceClient {
    Mono<Void> routeRequest(Map<String, Object> request, String url);

    Mono<Void> notifyError(ErrorResult request, String cmUrl);

    Mono<Void> routeResponse(JsonNode request, String cmUrl);
}
