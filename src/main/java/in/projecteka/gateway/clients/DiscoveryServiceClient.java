package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.Utils;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static in.projecteka.gateway.common.Constants.TEMP_CM_ID;

@AllArgsConstructor
public class DiscoveryServiceClient implements ServiceClient{
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServiceClient.class);
    private ServiceOptions serviceOptions;
    private WebClient.Builder webClientBuilder;
    private CMRegistry cmRegistry;
    private CentralRegistry centralRegistry;

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                .flatMap(serializedRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(url + "/v1/care-contexts/discover")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .bodyValue(serializedRequest)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                                .bodyToMono(Void.class)
                                .timeout(Duration.ofSeconds(serviceOptions.getTimeout()))));
    }

    @Override
    public Mono<Void> notifyError(ErrorResult request) {
        //TODO check backwhen cm id is dynamic
        Optional<YamlRegistryMapping> config = cmRegistry.getConfigFor(TEMP_CM_ID);
        if (config.isEmpty()) {
            logger.error("No mapping found for " + TEMP_CM_ID);
            return Mono.error(ClientError.mappingNotFoundForId(TEMP_CM_ID));
        }
        return centralRegistry.authenticate()
                .flatMap(token -> webClientBuilder.build()
                        .post()
                        .uri(config.get().getHost() + "/v1/care-contexts/on-discover")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                        .bodyToMono(Void.class));
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String cmUrl) {
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                        .flatMap(serializedRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(cmUrl + "/v1/care-contexts/on-discover")
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