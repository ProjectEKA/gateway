package in.projecteka.gateway.user;

import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
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

import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_FIND;
import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_ON_FIND;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;

@RestController
@AllArgsConstructor
public class UserController {
    RequestOrchestrator<PatientSearchServiceClient> patientSearchRequestOrchestrator;
    ResponseOrchestrator patientSearchResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENTS_FIND)
    public Mono<Void> findPatient(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        patientSearchRequestOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIU_ID, clientId)
                        .subscriberContext(context -> context.put("apiCalled", PATH_PATIENTS_FIND)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENTS_ON_FIND)
    public Mono<Void> onFindPatient(HttpEntity<String> requestEntity) {
        return patientSearchResponseOrchestrator.processResponse(requestEntity, X_HIU_ID)
                .subscriberContext(context -> context.put("apiCalled", PATH_PATIENTS_ON_FIND));
    }
}
