package in.projecteka.gateway.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class DiscoveryServiceClient {

    @Autowired
    private WebClient.Builder webClientBuilder;

    public Mono<Void> patientFor(String request, String url) {
        return webClientBuilder.build()
                .post()
                .uri(url + "/patients/discover/carecontexts")
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> Mono.error(ClientError.unableToConnectToProvider()))//TODO Error handling
                .bodyToMono(Void.class);
    }
}