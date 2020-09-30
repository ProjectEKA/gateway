package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.clients.IdentityServiceClient;
import in.projecteka.gateway.clients.model.Session;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.session.SessionRequest;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@AllArgsConstructor
public class IdentityService {

    private final IdentityServiceClient identityServiceClient;
    private final IdentityProperties properties;
    private final CacheAdapter<String, String> accessTokenCache;

    public Mono<String> authenticate() {
        return accessTokenCache.get(getKey())
                .switchIfEmpty(Mono.defer(this::tokenUsingSecret))
                .map(token -> format("%s %s", "Bearer", token));
    }

    private String getKey() {
        return "gateway:gateway:accessToken";
    }

    private Mono<String> tokenUsingSecret() {
        return identityServiceClient.getTokenFor(properties.getClientId(), properties.getClientSecret())
                .flatMap(session -> accessTokenCache.put(getKey(), session.getAccessToken())
                        .thenReturn(session.getAccessToken()));
    }

    public Mono<Session> getTokenFor(SessionRequest request) {
        return identityServiceClient.getTokenFor(request);
    }

    public Mono<JsonNode> configuration(String host) {
        return Mono.fromCallable(() -> new ObjectMapper().readTree(
                format("{\"jwks_uri\": \"%s" + Constants.CURRENT_VERSION + "/certs\"}", host)));
    }

    public Mono<JsonNode> certs() {
        return identityServiceClient.certs();
    }

    public Mono<String> tokenForAdmin() {
        return identityServiceClient.getUserToken(properties.getClientId(),
                properties.getClientSecret(),
                properties.getUserName(),
                properties.getPassword())
                .map(session -> format("%s %s", session.getTokenType(), session.getAccessToken()));
    }
}
