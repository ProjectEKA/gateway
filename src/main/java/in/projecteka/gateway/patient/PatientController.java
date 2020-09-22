package in.projecteka.gateway.patient;

import in.projecteka.gateway.clients.PatientServiceClient;
import in.projecteka.gateway.common.Caller;
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

import static in.projecteka.gateway.common.Constants.API_CALLED;
import static in.projecteka.gateway.common.Constants.PATH_PATIENT_ON_SHARE;
import static in.projecteka.gateway.common.Constants.PATH_PATIENT_SHARE;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;

@RestController
@AllArgsConstructor
public class PatientController {
    RequestOrchestrator<PatientServiceClient> patientRequestOrchestrator;
    ResponseOrchestrator patientResponseOrchestrator;

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @PostMapping(PATH_PATIENT_SHARE)
    public Mono<Void> patientProfileShare(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        patientRequestOrchestrator.handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId)
                                .subscriberContext(context -> context.put(API_CALLED, PATH_PATIENT_SHARE)));

    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @PostMapping(PATH_PATIENT_ON_SHARE)
    public Mono<Void> patientProfileOnShare(HttpEntity<String> requestEntity) {
        return patientResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_PATIENT_ON_SHARE));
    }
}
