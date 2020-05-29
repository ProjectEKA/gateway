package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.model.ErrorResult;
import reactor.core.publisher.Mono;

import java.util.Map;

public class ConsentRequestServiceClient implements ServiceClient{
    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return null;
    }

    @Override
    public Mono<Void> notifyError(ErrorResult request, String cmUrl) {
        return null;
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String cmUrl) {
        return null;
    }
}
