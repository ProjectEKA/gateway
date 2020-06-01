package in.projecteka.gateway.consent;

import in.projecteka.gateway.clients.ConsentFetchServiceClient;
import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;

@RestController
@AllArgsConstructor
public class ConsentController {
    RequestOrchestrator<ConsentRequestServiceClient> consentRequestOrchestrator;
    ResponseOrchestrator consentResponseOrchestrator;
    RequestOrchestrator<ConsentFetchServiceClient> consentFetchOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consent-requests/init")
    public Mono<Void> createConsentRequest(HttpEntity<String> requestEntity) {
        Mono<Void> toBeFiredAndForgotten = consentRequestOrchestrator.processRequest(requestEntity, X_CM_ID);
        toBeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consent-requests/on-init")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        return consentResponseOrchestrator.processResponse(requestEntity, X_HIU_ID);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consents/fetch")
    public Mono<Void> fetchConsent(HttpEntity<String> requestEntity) {
        Mono<Void> toBeFiredAndForgotten = consentFetchOrchestrator.processRequest(requestEntity, X_CM_ID);
        toBeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
