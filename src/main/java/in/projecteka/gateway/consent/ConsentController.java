package in.projecteka.gateway.consent;

import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.clients.ConsentFetchServiceClient;
import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static in.projecteka.gateway.common.Utils.requestInfoLog;
import static in.projecteka.gateway.common.Utils.responseInfoLog;

@RestController
@AllArgsConstructor
public class ConsentController {
    private static final Logger logger = LoggerFactory.getLogger(ConsentController.class);

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
                .flatMap(clientId -> {
                    requestInfoLog(requestEntity, clientId
                            , X_HIU_ID, X_CM_ID
                            , V_1_CONSENT_REQUESTS_INIT);
                    logger.info("Consent Request init");

                    return consentRequestOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIU_ID, clientId);
                });
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENT_REQUESTS_ON_INIT)
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        responseInfoLog(requestEntity, "CM", X_HIU_ID, V_1_CONSENT_REQUESTS_ON_INIT);
        logger.info("Consent Request on-init");

        return consentResponseOrchestrator.processResponse(requestEntity, X_HIU_ID);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENTS_FETCH)
    public Mono<Void> fetchConsent(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> {
                    requestInfoLog(requestEntity, X_HIU_ID, X_CM_ID, clientId, V_1_CONSENTS_FETCH);
                    logger.info("Consent fetch");

                    return consentFetchRequestOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIU_ID, clientId);
                });
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENTS_ON_FETCH)
    public Mono<Void> onFetchConsent(HttpEntity<String> requestEntity) {
        responseInfoLog(requestEntity, "CM", X_HIU_ID, V_1_CONSENTS_ON_FETCH);
        logger.info("Consent on-fetch");

        return consentFetchResponseOrchestrator.processResponse(requestEntity, X_HIU_ID);
    }
}
