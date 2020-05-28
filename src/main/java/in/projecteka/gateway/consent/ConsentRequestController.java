package in.projecteka.gateway.consent;

import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.X_CM_ID;

@RestController
@AllArgsConstructor
public class ConsentRequestController {
    RequestOrchestrator<ConsentRequestServiceClient> consentRequestOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consent-requests/init")
    public Mono<Void> discoverCareContext(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = consentRequestOrchestrator.processRequest(requestEntity, X_CM_ID);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consent-requests/on-init")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }
}
