package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.Utils;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.registry.BridgeRegistry;
import lombok.AllArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static in.projecteka.gateway.common.Constants.TEMP_HIU_ID;

@AllArgsConstructor
public class ConsentRequestServiceClient implements ServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(ConsentRequestServiceClient.class);
    private ServiceOptions serviceOptions;
    private WebClient.Builder webClientBuilder;
    private BridgeRegistry bridgeRegistry;
    private CentralRegistry centralRegistry;

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                        .flatMap(serializedRequest ->
                                webClientBuilder.build()
                                        .post()
                                        .uri(url + "/v1/consent-requests/init")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .bodyValue(serializedRequest)
                                        .retrieve()
                                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                                clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO
                                        // Errorhandling
                                        .bodyToMono(Void.class)
                                        .timeout(Duration.ofSeconds(serviceOptions.getTimeout()))));
    }

    @Override
    public Mono<Void> notifyError(ErrorResult request) {
        //TODO check back when hiu id is dynamic
        Optional<YamlRegistryMapping> config = bridgeRegistry.getConfigFor(TEMP_HIU_ID, ServiceType.HIU);
        if (config.isEmpty()) {
            logger.error("No mapping found for " + TEMP_HIU_ID);
            return Mono.error(ClientError.mappingNotFoundForId(TEMP_HIU_ID));
        }
        return centralRegistry.authenticate()
                .flatMap(token -> webClientBuilder.build()
                        .post()
                        .uri(config.get().getHost() + "/v1/consent-requests/on-init")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                        .bodyToMono(Void.class));
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String url) {
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                        .flatMap(serializedRequest ->
                                webClientBuilder.build()
                                        .post()
                                        .uri(url + "/v1/consent-requests/on-init")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .bodyValue(serializedRequest)
                                        .retrieve()
                                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                                clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                                        .bodyToMono(Void.class)
                                        .timeout(Duration.ofSeconds(serviceOptions.getTimeout()))));
    }
}
