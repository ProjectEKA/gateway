package in.projecteka.gateway.consent;

import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.clients.ConsentFetchServiceClient;
import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_FETCH;
import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_ON_FETCH;
import static in.projecteka.gateway.common.Constants.V_1_CONSENT_REQUESTS_INIT;
import static in.projecteka.gateway.common.Constants.V_1_CONSENT_REQUESTS_ON_INIT;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;

@RestController
@AllArgsConstructor
public class ConsentController {
    RequestOrchestrator<ConsentRequestServiceClient> consentRequestOrchestrator;
    ResponseOrchestrator consentResponseOrchestrator;
    RequestOrchestrator<ConsentFetchServiceClient> consentFetchRequestOrchestrator;
    ResponseOrchestrator consentFetchResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENT_REQUESTS_INIT)
    public Mono<Void> createConsentRequest(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> consentRequestOrchestrator.handleThis(requestEntity, X_CM_ID, clientId));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENT_REQUESTS_ON_INIT)
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        return consentResponseOrchestrator.processResponse(requestEntity, X_HIU_ID);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENTS_FETCH)
    public Mono<Void> fetchConsent(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> consentFetchRequestOrchestrator.handleThis(requestEntity, X_CM_ID, clientId));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENTS_ON_FETCH)
    public Mono<Void> onFetchConsent(HttpEntity<String> requestEntity) {
        return consentFetchResponseOrchestrator.processResponse(requestEntity, X_HIU_ID);
    }
}
