package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.clients.ClientError.unableToConnect;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Serializer.from;
import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public abstract class ServiceClient {
    public static final String NO_MAPPING_FOUND_FOR_CLIENT = "No mapping found for %s";
    private static final Logger logger = LoggerFactory.getLogger(ServiceClient.class);

    protected final ServiceOptions serviceOptions;
    protected final WebClient.Builder webClientBuilder;
    protected final IdentityService identityService;

    public Mono<Void> routeRequest(Map<String, Object> request, String clientId, String routingKey) {
        return routeCommon(request, clientId, this::getRequestUrl, routingKey);
    }

    public Mono<Void> routeResponse(JsonNode request, String clientId, String routingKey) {
        return routeCommon(request, clientId, this::getResponseUrl, routingKey);
    }

    public Mono<Void> notifyError(String clientId, String sourceRoutingKey, ErrorResult request) {
        return routeCommon(request, clientId, this::getResponseUrl, sourceRoutingKey);
    }

    protected abstract Optional<String> getResponseUrl(String clientId);

    protected abstract Optional<String> getRequestUrl(String clientId);


    private <T> Mono<Void> routeCommon(T requestBody,
                                       String clientId,
                                       Function<String, Optional<String>> urlGetter,
                                       String routingKey) {
        return urlGetter.apply(clientId)
                .map(url -> from(requestBody).map(serialized -> route(serialized, url, routingKey, clientId)).orElse(empty()))
                .orElseGet(() -> {
                    logger.error(format(NO_MAPPING_FOUND_FOR_CLIENT, clientId));
                    return error(mappingNotFoundForId(clientId));
                });
    }

    private <T> Mono<Void> route(T request, String url, String routingKey, String clientId) {

        MDC.put("path", url);
        MDC.put("method", "POST");
        if (routingKey.equals(X_HIP_ID) || routingKey.equals(X_HIU_ID)) {
            MDC.put("target-id", clientId);
            logger.info("Target service info");
            return identityService.authenticate()
                    .flatMap(token -> bridgeWebClientBuilder(request, url, token, routingKey, clientId)).then();
        }
        logger.info("Target service info");
        MDC.clear();
        return identityService.authenticate()
                .flatMap(token -> cmWebClientBuilder(request, url, token)).then();
    }

    private <T> Mono<ResponseEntity<Void>> cmWebClientBuilder(T request, String url, String token) {
        return webClientBuilder.build()
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
                .timeout(Duration.ofSeconds(serviceOptions.getTimeout()));
    }

    private <T> Mono<ResponseEntity<Void>> bridgeWebClientBuilder(T request,
                                                                  String url,
                                                                  String token,
                                                                  String routingKey,
                                                                  String clientId) {
        return webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .header(routingKey, clientId)
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> clientResponse
                                .bodyToMono(HashMap.class)
                                .doOnSuccess(e -> logger.error(clientResponse.statusCode().toString(),
                                        e.toString()))
                                .then(error(unableToConnect())))
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(serviceOptions.getTimeout()));
    }
}
