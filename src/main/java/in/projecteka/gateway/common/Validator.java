package in.projecteka.gateway.common;

import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import static in.projecteka.gateway.clients.ClientError.invalidRequest;
import static in.projecteka.gateway.clients.ClientError.mappingNotFoundForId;
import static in.projecteka.gateway.clients.ClientError.tooManyRequests;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.TIMESTAMP;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Serializer.deserializeRequestAsJsonNode;
import static in.projecteka.gateway.registry.ServiceType.HIP;
import static in.projecteka.gateway.registry.ServiceType.HIU;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.springframework.util.StringUtils.hasText;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
public class Validator {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    private static final String RESP_REQUEST_ID_IS_NULL_OR_EMPTY = "resp.requestId is null or empty";
    private static final String HEADER_NOT_FOUND = "No {} found on Headers";
    private static final String NO_MAPPING_FOUND_FOR_ROUTING_KEY = "No mapping found for {} : {}";

    BridgeRegistry bridgeRegistry;
    CMRegistry cmRegistry;
    CacheAdapter<String, String> requestIdMappings;
    CacheAdapter<String, String> requestIdTimestampMappings;

    private static Mono<ValidatedRequest> toRequest(HttpEntity<String> maybeRequest, String clientId) {
        return Serializer.from(maybeRequest)
                .filter(request -> hasText((String) request.get(REQUEST_ID)))
                .flatMap(request -> from((String) request.get(REQUEST_ID))
                        .map(requestUUID -> just(new ValidatedRequest(requestUUID, request, clientId))))
                .orElseGet(() -> {
                    var errorMessage = format("Empty/Invalid %s found on the payload", REQUEST_ID);
                    logger.error(errorMessage);
                    return error(invalidRequest(errorMessage));
                });
    }

    private static Optional<UUID> from(String requestId) {
        try {
            return of(UUID.fromString(requestId));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return empty();
    }

    private static Optional<YamlRegistryMapping> getRegistryMapping(BridgeRegistry bridgeRegistry,
                                                                    CMRegistry cmRegistry,
                                                                    String routingHeaderKey,
                                                                    String clientId) {
        if (routingHeaderKey.equals(X_HIP_ID)) {
            return bridgeRegistry.getConfigFor(clientId, HIP);
        }
        if (routingHeaderKey.equals(X_HIU_ID)) {
            return bridgeRegistry.getConfigFor(clientId, HIU);
        }
        return cmRegistry.getConfigFor(clientId);
    }

    public Mono<ValidatedRequest> validateRequest(HttpEntity<String> maybeRequest, String routingKey) {
        return validateReq(maybeRequest, routingKey, Validator::toRequest);
    }

    public Mono<ValidatedResponse> validateResponse(HttpEntity<String> maybeResponse, String routingKey) {
        return validate(maybeResponse, routingKey, this::toResponse);
    }

    private <T> Mono<T> validateReq(HttpEntity<String> maybeRequest,
                                    String routingKey,
                                    BiFunction<HttpEntity<String>, String, Mono<T>> to) {
        return Mono.just(maybeRequest)
                .filterWhen(this::isValidRequest)
                .switchIfEmpty(error(tooManyRequests()))
                .flatMap(val -> validate(maybeRequest, routingKey, to));
    }

    private <T> Mono<T> validate(HttpEntity<String> maybeRequest,
                                 String routingKey,
                                 BiFunction<HttpEntity<String>, String, Mono<T>> to) {
        String clientId = maybeRequest.getHeaders().getFirst(routingKey);
        if (!hasText(clientId)) {
            logger.error(HEADER_NOT_FOUND, routingKey);
            return error(mappingNotFoundForId(routingKey));
        }
        return getRegistryMapping(bridgeRegistry, cmRegistry, routingKey, clientId)
                .map(registry -> to.apply(maybeRequest, registry.getId()))
                .orElseGet(() -> {
                    logger.error(NO_MAPPING_FOUND_FOR_ROUTING_KEY, routingKey, clientId);
                    return error(mappingNotFoundForId(routingKey));
                });
    }

    private Mono<ValidatedResponse> toResponse(HttpEntity<String> maybeResponse, String clientId) {
        return deserializeRequestAsJsonNode(maybeResponse)
                .filter(jsonNode -> !jsonNode.path("resp").path(REQUEST_ID).asText().isEmpty())
                .switchIfEmpty(defer(() -> {
                    logger.error(RESP_REQUEST_ID_IS_NULL_OR_EMPTY);
                    return error(invalidRequest(RESP_REQUEST_ID_IS_NULL_OR_EMPTY));
                }))
                .flatMap(jsonNode -> {
                    var respRequestId = jsonNode.path("resp").path(REQUEST_ID).asText();
                    return requestIdMappings.get(respRequestId)
                            .filter(StringUtils::hasText)
                            .switchIfEmpty(error(invalidRequest("No mapping found for resp.requestId on cache")))
                            .map(callerRequestId -> {
                                logger.info("Received a response {} {} {}", keyValue("requestId", callerRequestId),
                                        keyValue("gatewayId", respRequestId),
                                        keyValue("targetId", clientId));
                                return new ValidatedResponse(clientId, callerRequestId, jsonNode);
                            });
                });
    }

    private Mono<Boolean> isValidRequest(HttpEntity<String> maybeRequest) {
        return isRequestIdPresent(maybeRequest)
                .flatMap(result -> error(tooManyRequests()))
                .then(isRequestIdValidInGivenTimestamp(maybeRequest));
    }

    private Mono<Boolean> isRequestIdPresent(HttpEntity<String> maybeRequest) {
        return getValue(maybeRequest, REQUEST_ID)
                .flatMap(requestId -> requestIdTimestampMappings.get(requestId))
                .map(StringUtils::hasText);
    }

    private Mono<String> getValue(HttpEntity<String> maybeRequest, String key) {
        return Serializer.from(maybeRequest)
                .filter(request -> hasText((String) request.get(key)))
                .flatMap(request -> of((String) request.get(key))
                        .map(Mono::just))
                .orElseGet(() -> {
                    var errorMessage = "Invalid request";
                    logger.error(errorMessage);
                    return error(invalidRequest(errorMessage));
                });
    }

    private Mono<Boolean> isRequestIdValidInGivenTimestamp(HttpEntity<String> maybeRequest) {
        return getValue(maybeRequest, TIMESTAMP)
                .map(timestamp -> isValidTimestamp(toDate(timestamp)));
    }

    private LocalDateTime toDate(String timestamp) {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart().appendOffsetId()
                .toFormatter();
        return LocalDateTime.parse(timestamp, dateTimeFormatter);
    }

    private boolean isValidTimestamp(LocalDateTime timestamp) {
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startTime = currentTime.minusMinutes(1);
        LocalDateTime endTime = currentTime.plusMinutes(9);
        return timestamp.isAfter(startTime) && timestamp.isBefore(endTime);
    }
}
