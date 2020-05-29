package in.projecteka.gateway.consent;

import in.projecteka.gateway.clients.ConsentArtefactServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.X_HIP_ID;


@RestController
@AllArgsConstructor
public class ConsentArtefactController {
    RequestOrchestrator<ConsentArtefactServiceClient> consentArtefactHipNotifyOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/consents/hip/notify")
    public Mono<Void> createConsentRequest(HttpEntity<String> requestEntity) {
        Mono<Void> toBeFiredAndForgotten = consentArtefactHipNotifyOrchestrator.processRequest(requestEntity, X_HIP_ID);
        toBeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
