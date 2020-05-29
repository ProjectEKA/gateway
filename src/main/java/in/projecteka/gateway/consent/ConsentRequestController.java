package in.projecteka.gateway.consent;

import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

public class ConsentRequestController {
    RequestOrchestrator<ConsentRequestServiceClient> consentRequestOrchestrator;
    ResponseOrchestrator consentResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consent-requests/init")
    public Mono<Void> discoverCareContext(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consent-requests/on-init")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }
}
