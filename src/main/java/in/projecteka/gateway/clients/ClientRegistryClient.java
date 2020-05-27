package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Properties;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class ClientRegistryClient {

    private final Logger logger = LoggerFactory.getLogger(ClientRegistryClient.class);
    private final WebClient.Builder webClientBuilder;

    public ClientRegistryClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClientBuilder = webClientBuilder.baseUrl(baseUrl);
    }

    public Mono<Session> getTokenFor(String clientId, String clientSecret) {
        return webClientBuilder.build()
                .post()
                .uri("/api/1.0/sessions")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestWith(clientId, clientSecret)))
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(Properties.class)
                        .doOnNext(properties -> logger.error(properties.toString()))
                        .thenReturn(ClientError.unAuthorized()))
                .bodyToMono(Session.class);
    }

    private SessionRequest requestWith(String clientId, String clientSecret) {
        return new SessionRequest(clientId, clientSecret, "password");
    }

    @AllArgsConstructor
    @Data
    private static class SessionRequest {
        private String clientId;
        private String clientSecret;
        private String grantType;
    }
}

