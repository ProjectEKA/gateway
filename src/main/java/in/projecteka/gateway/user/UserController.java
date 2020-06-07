package in.projecteka.gateway.user;

import in.projecteka.gateway.clients.PatientSearchServiceClient;
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
public class UserController {
    RequestOrchestrator<PatientSearchServiceClient> patientSearchServiceClient;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/patients/find")
    public Mono<Void> findPatient(HttpEntity<String> requestEntity) {
        Mono<Void> toBeFiredAndForgotten = patientSearchServiceClient.processRequest(requestEntity, X_CM_ID);
        toBeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
