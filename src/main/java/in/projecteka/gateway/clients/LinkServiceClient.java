package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.link.discovery.Utils;
import in.projecteka.gateway.link.link.model.LinkInitResult;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@AllArgsConstructor
public class LinkServiceClient {
    private WebClient.Builder webClientBuilder;
    private ServiceOptions serviceOptions;

    public Mono<Void> linkInit(Map<String, Object> request, String url) {
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

    public Mono<Void> linkInitErrorResultNotify(LinkInitResult request, String cmUrl) {
        return webClientBuilder.build()
                .post()
                .uri(cmUrl + "/v1/links/link/on-init")
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                .bodyToMono(Void.class);
    }

    public Mono<Void> linkOnInitResultNotify(JsonNode request, String cmUrl) {
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