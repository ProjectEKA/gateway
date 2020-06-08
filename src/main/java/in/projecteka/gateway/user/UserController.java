package in.projecteka.gateway.user;

import in.projecteka.gateway.clients.Caller;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.X_CM_ID;

@RestController
@AllArgsConstructor
public class UserController {
    RequestOrchestrator<PatientSearchServiceClient> patientSearchRequestOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/patients/find")
    public Mono<Void> findPatient(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> patientSearchRequestOrchestrator.handleThis(requestEntity, X_CM_ID, clientId));
    }
}
