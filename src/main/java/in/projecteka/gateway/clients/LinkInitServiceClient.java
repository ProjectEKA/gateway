package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.Utils;
import in.projecteka.gateway.common.model.ErrorResult;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@AllArgsConstructor
public class LinkInitServiceClient implements ServiceClient{
    private WebClient.Builder webClientBuilder;
    private ServiceOptions serviceOptions;
    private CentralRegistry centralRegistry;

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                        .flatMap(serializedRequest ->
                                webClientBuilder.build()
                                        .post()
                                        .uri(url + "/v1/links/link/init")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .bodyValue(serializedRequest)
                                        .retrieve()
                                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Errorhandling
                                .bodyToMono(Void.class)
                                .timeout(Duration.ofSeconds(serviceOptions.getTimeout()))));
    }

    @Override
    public Mono<Void> notifyError(ErrorResult request, String cmUrl) {
        return centralRegistry.authenticate()
                .flatMap(token ->
                        webClientBuilder.build()
                                .post()
                                .uri(cmUrl + "/v1/links/link/on-init")
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .bodyValue(request)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))
                                .bodyToMono(Void.class));
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String cmUrl) {
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                        .flatMap(serializedRequest ->
                                webClientBuilder.build()
                                        .post()
                                        .uri(cmUrl + "/v1/links/link/on-init")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .bodyValue(serializedRequest)
                                        .retrieve()
                                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                                clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Errorhandling
                                        .bodyToMono(Void.class)
                                        .timeout(Duration.ofSeconds(serviceOptions.getTimeout()))));
    }
}