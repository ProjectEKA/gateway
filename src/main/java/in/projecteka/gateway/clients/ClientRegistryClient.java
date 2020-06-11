package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.model.Session;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static in.projecteka.gateway.common.Serializer.from;
import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

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

    @AllArgsConstructor
    public abstract static class ServiceClient {
        private static final Logger logger = LoggerFactory.getLogger(ServiceClient.class);

        protected final ServiceOptions serviceOptions;
        protected final WebClient.Builder webClientBuilder;
        protected final CentralRegistry centralRegistry;

        public Mono<Void> routeRequest(Map<String, Object> request, String url) {
            return routeCommon(request, url);
        }

        public Mono<Void> routeResponse(JsonNode request, String url) {
            return routeCommon(request, url);
        }

        public Mono<Void> notifyError(String clientId, ErrorResult request) {
            return getResponseUrl(clientId)
                    .map(url -> route(request, url))
                    .orElseGet(() -> {
                        logger.error(format("No mapping found for %s", clientId));
                        return Mono.error(ClientError.mappingNotFoundForId(clientId));
                    });
        }

        protected abstract Optional<String> getResponseUrl(String clientId);

        private <T> Mono<Void> routeCommon(T requestBody, String url) {
            return from(requestBody)
                    .map(serialized -> route(serialized, url))
                    .orElse(empty());
        }

        private <T> Mono<Void> route(T request, String url) {
            return centralRegistry.authenticate()
                    .flatMap(token ->
                            webClientBuilder.build()
                                    .post()
                                    .uri(url)
                                    .contentType(APPLICATION_JSON)
                                    .header(AUTHORIZATION, token)
                                    .bodyValue(request)
                                    .retrieve()
                                    .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                            clientResponse -> error(ClientError.unableToConnect()))
                                    .toBodilessEntity()
                                    .timeout(Duration.ofSeconds(serviceOptions.getTimeout())))
                    .then();
        }
    }
}

