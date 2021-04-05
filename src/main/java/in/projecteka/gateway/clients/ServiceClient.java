package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.model.CmErrorResponse;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.registry.ServiceType;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import static in.projecteka.gateway.clients.ClientError.invalidRequest;
import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.clients.ClientError.unableToConnect;
import static in.projecteka.gateway.common.Constants.CORRELATION_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Constants.X_ORIGIN_ID;
import static in.projecteka.gateway.common.Serializer.from;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
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

    public Mono<Void> routeRequest(Map<String, Object> request, String clientId, String routingKey, String sourceId) {
        return routeCommon(request, clientId, this::getRequestUrl, routingKey, sourceId);
    }

    public Mono<Void> routeResponse(JsonNode request, String clientId, String routingKey) {
        return routeCommon(request, clientId, this::getResponseUrl, routingKey, null);
    }

    public Mono<Void> notifyError(String clientId, String sourceRoutingKey, ErrorResult request) {
        return routeCommon(request, clientId, this::getResponseUrl, sourceRoutingKey, null);
    }

    protected abstract Mono<String> getResponseUrl(String clientId, ServiceType routingKey);

    protected abstract Mono<String> getRequestUrl(String clientId, ServiceType routingKey);


    private <T> Mono<Void> routeCommon(T requestBody,
                                       String targetId,
                                       BiFunction<String,ServiceType, Mono<String>> urlGetter,
                                       String routingKey,
                                       String sourceId) {
        var serviceType = routingKey.equals(X_HIP_ID)? ServiceType.HIP : ServiceType.HIU;
        return urlGetter.apply(targetId, serviceType)
                .switchIfEmpty(Mono.defer(() -> {
                    logger.error(format(NO_MAPPING_FOUND_FOR_CLIENT, targetId));
                    return error(mappingNotFoundForId(targetId));
                }))
                .flatMap(url -> from(requestBody)
                        .map(serialized -> route(serialized, url, routingKey, targetId, sourceId))
                        .orElse(empty()))
                .subscriberContext(ctx -> {
                    Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                    return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                            .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                });

    }

    private <T> Mono<Void> route(T request, String url, String routingKey, String targetId, String sourceId) {
        return routingKey.equals(X_HIP_ID) || routingKey.equals(X_HIU_ID)
               ? identityService.authenticate()
                       .flatMap(token -> bridgeWebClientBuilder(request, url, token, routingKey, targetId)).then()
               : identityService.authenticate()
                       .flatMap(token -> cmWebClientBuilder(request, url, token, sourceId)).then();
    }

    private <T> Mono<ResponseEntity<Void>> cmWebClientBuilder(T request, String url, String token, String sourceId) {
        return webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .header(CORRELATION_ID, MDC.get(CORRELATION_ID))
                .header(X_ORIGIN_ID, sourceId)
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> clientResponse
                                .bodyToMono(CmErrorResponse.class)
                                .doOnSuccess(e -> logger.error("Error: {}, {}", clientResponse.statusCode(), e))
                                .flatMap(cmErrorResponse -> error(invalidRequest(cmErrorResponse.getError().getMessage())))
                )
                .toBodilessEntity()
                .publishOn(Schedulers.elastic())
                .doOnSubscribe(subscription -> logger.info("About to call cm for source {}, url {}", sourceId, url))
                .timeout(ofSeconds(serviceOptions.getTimeout()));
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
                .header(CORRELATION_ID, MDC.get(CORRELATION_ID))
                .header(routingKey, clientId)
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(HashMap.class)
                                .doOnSuccess(e -> logger.error("Error: {} {}", clientResponse.statusCode(), e))
                                .then(error(unableToConnect())))
                .toBodilessEntity()
                .publishOn(Schedulers.elastic())
                .doOnSubscribe(subscription -> logger.info("About to call bridge {} for url {}", clientId, url))
                .timeout(ofSeconds(serviceOptions.getTimeout()));
    }
}
