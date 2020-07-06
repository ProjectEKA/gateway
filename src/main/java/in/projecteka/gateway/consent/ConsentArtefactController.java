package in.projecteka.gateway.consent;

import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.clients.HipConsentNotifyServiceClient;
import in.projecteka.gateway.clients.HiuConsentNotifyServiceClient;
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

import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_HIP_NOTIFY;
import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_HIU_NOTIFY;
import static in.projecteka.gateway.common.Constants.V_1_CONSENTS_HIP_ON_NOTIFY;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;


@RestController
@AllArgsConstructor
public class ConsentArtefactController {
    RequestOrchestrator<HipConsentNotifyServiceClient> hipConsentNotifyRequestOrchestrator;
    RequestOrchestrator<HiuConsentNotifyServiceClient> hiuConsentNotifyRequestOrchestrator;
    ResponseOrchestrator hipConsentNotifyResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENTS_HIP_NOTIFY)
    public Mono<Void> consentNotifyToHIP(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> hipConsentNotifyRequestOrchestrator
                        .handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId)
                        .subscriberContext(context -> context.put("apiCalled",V_1_CONSENTS_HIP_NOTIFY)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENTS_HIP_ON_NOTIFY)
    public Mono<Void> consentOnNotifyToHIP(HttpEntity<String> requestEntity) {
        return hipConsentNotifyResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                        .subscriberContext(context -> context.put("apiCalled",V_1_CONSENTS_HIP_ON_NOTIFY));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_CONSENTS_HIU_NOTIFY)
    public Mono<Void> consentNotifyToHIU(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> hiuConsentNotifyRequestOrchestrator
                        .handleThis(requestEntity, X_HIU_ID, X_CM_ID, clientId))
                                .subscriberContext(context -> context.put("apiCalled",V_1_CONSENTS_HIU_NOTIFY));
    }
}
