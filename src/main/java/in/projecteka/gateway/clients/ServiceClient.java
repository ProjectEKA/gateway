package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.Utils;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@AllArgsConstructor
public abstract class ServiceClient {
    protected ServiceOptions serviceOptions;
    protected WebClient.Builder webClientBuilder;
    protected CentralRegistry centralRegistry;

    private Mono<Void> route(String serializedRequest, String url, String token){
        return webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(serializedRequest)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> Mono.error(ClientError.unableToConnect()))
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(serviceOptions.getTimeout()));
    }

    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                        .flatMap(serializedRequest -> route(serializedRequest, url, token)));
    }

    public abstract Mono<Void> notifyError(ErrorResult errorResult);

    public Mono<Void> notifyError(ErrorResult request, String url){
        return centralRegistry.authenticate()
                .flatMap(token -> webClientBuilder.build()
                        .post()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                        .bodyToMono(Void.class));
    };

    public Mono<Void> routeResponse(JsonNode request, String url){
        return centralRegistry.authenticate()
                .flatMap(token -> Utils.serializeRequest(request)
                        .flatMap(serializedRequest -> route(serializedRequest, url, token)));
    }
}
