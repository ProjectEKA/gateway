package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.link.discovery.Utils;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@AllArgsConstructor
public class LinkOnInitServiceClient {
    private WebClient.Builder webClientBuilder;
    private ServiceOptions serviceOptions;

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
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                                .bodyToMono(Void.class)
                                .timeout(Duration.ofSeconds(serviceOptions.getTimeout())));
    }
}
