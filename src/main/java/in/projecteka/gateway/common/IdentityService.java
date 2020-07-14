package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.IdentityServiceClient;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.clients.model.Session;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@AllArgsConstructor
public class IdentityService {

    private final IdentityServiceClient identityServiceClient;

    private final IdentityProperties properties;

    public Mono<String> authenticate() {
        return identityServiceClient
                .getTokenFor(properties.getClientId(), properties.getClientSecret())
                .map(session -> format("%s %s", session.getTokenType(), session.getAccessToken()));
    }

    public Mono<Session> getTokenFor(String clientId, String clientSecret) {
        return identityServiceClient.getTokenFor(clientId, clientSecret);
    }

    public Mono<JsonNode> configuration(String host) {
        return Mono.fromCallable(() -> new ObjectMapper().readTree(
                format("{\"jwks_uri\": \"%s" + Constants.CURRENT_VERSION + "/certs\"}", host)));
    }

    public Mono<JsonNode> certs() {
        return identityServiceClient.certs();
    }
}
