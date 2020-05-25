package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.link.common.Utils;
import in.projecteka.gateway.link.common.model.ErrorResult;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@AllArgsConstructor
public class LinkServiceClient implements ServiceClient{
    private WebClient.Builder webClientBuilder;
    private ServiceOptions serviceOptions;

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return Utils.serializeRequest(request)
                .flatMap(serializedRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(url + "/v1/links/link/init")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(serializedRequest)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Errorhandling
                                .bodyToMono(Void.class)
                                .timeout(Duration.ofSeconds(serviceOptions.getTimeout())));
    }

    @Override
    public Mono<Void> notifyError(ErrorResult request, String cmUrl) {
        return webClientBuilder.build()
                .post()
                .uri(cmUrl + "/v1/links/link/on-init")
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                .bodyToMono(Void.class);
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String cmUrl) {
        return Utils.serializeRequest(request)
                .flatMap(serializedRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(cmUrl + "/v1/links/link/on-init")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(serializedRequest)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Errorhandling
                                .bodyToMono(Void.class)
                                .timeout(Duration.ofSeconds(serviceOptions.getTimeout())));
    }
}