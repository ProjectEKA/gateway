package in.projecteka.gateway.consent;

import in.projecteka.gateway.clients.HipConsentNotifyServiceClient;
import in.projecteka.gateway.clients.HiuConsentNotifyServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.TEMP_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;


@RestController
@AllArgsConstructor
public class ConsentArtefactController {
    RequestOrchestrator<HipConsentNotifyServiceClient> hipConsentNotifyRequestOrchestrator;
    RequestOrchestrator<HiuConsentNotifyServiceClient> hiuConsentNotifyRequestOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consents/hip/notify")
    public Mono<Void> consentNotifyToHIP(HttpEntity<String> requestEntity) {
        hipConsentNotifyRequestOrchestrator.processRequest(requestEntity, X_HIP_ID, TEMP_CM_ID).subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consents/hiu/notify")
    public Mono<Void> consentNotifyToHIU(HttpEntity<String> requestEntity) {
        hiuConsentNotifyRequestOrchestrator.processRequest(requestEntity, X_HIU_ID, TEMP_CM_ID).subscribe();
        return Mono.empty();
    }
}
