package in.projecteka.gateway.common;

import in.projecteka.gateway.clients.ClientRegistryClient;
import in.projecteka.gateway.clients.ClientRegistryProperties;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@AllArgsConstructor
public class CentralRegistry {
    private final ClientRegistryClient clientRegistryClient;
    private final ClientRegistryProperties properties;

    public Mono<String> authenticate() {
        return clientRegistryClient
                .getTokenFor(properties.getClientId(), properties.getClientSecret())
                .map(session -> format("%s %s", session.getTokenType(), session.getAccessToken()));
    }
}
