package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.clients.ClientError.unableToConnect;
import static in.projecteka.gateway.common.Serializer.from;
import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public abstract class ServiceClient {
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
                    return error(mappingNotFoundForId(clientId));
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
                                        clientResponse -> clientResponse
                                                .bodyToMono(HashMap.class)
                                                .doOnSuccess(e -> logger.error(clientResponse.statusCode().toString(),
                                                        e.toString()))
                                                .then(error(unableToConnect())))
                                .toBodilessEntity()
                                .timeout(Duration.ofSeconds(serviceOptions.getTimeout())))
                .then();
    }
}
