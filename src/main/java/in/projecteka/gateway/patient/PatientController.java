package in.projecteka.gateway.patient;

import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.PatientSMSNotificationClient;
import in.projecteka.gateway.clients.PatientServiceClient;
import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.ShareProfile;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.API_CALLED;
import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_SMS_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_SMS_ON_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_PATIENT_ON_SHARE;
import static in.projecteka.gateway.common.Constants.PATH_PATIENT_SHARE;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;

@RestController
@AllArgsConstructor
public class PatientController {
    RequestOrchestrator<PatientServiceClient> patientRequestOrchestrator;
    ResponseOrchestrator patientResponseOrchestrator;
    RequestOrchestrator<PatientSMSNotificationClient> patientSMSNotifyRequestOrchestrator;
    ResponseOrchestrator patientSMSNotifyResponseOrchestrator;

    ShareProfile shareProfileFeature;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENT_SHARE)
    public Mono<Void> patientProfileShare(HttpEntity<String> requestEntity) {
        if(!shareProfileFeature.isEnable()) {
            return Mono.error(ClientError.notFound("Request not found"));
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        patientRequestOrchestrator.handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId)
                                .subscriberContext(context -> context.put(API_CALLED, PATH_PATIENT_SHARE)));

    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENT_ON_SHARE)
    public Mono<Void> patientProfileOnShare(HttpEntity<String> requestEntity) {
        if(!shareProfileFeature.isEnable()) {
            return Mono.error(ClientError.notFound("Request not found"));
        }
        return patientResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_PATIENT_ON_SHARE));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENTS_SMS_NOTIFY)
    public Mono<Void> sendSMSNotify(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        patientSMSNotifyRequestOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIP_ID, clientId)
                                .subscriberContext(context -> context.put(API_CALLED, PATH_PATIENTS_SMS_NOTIFY)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENTS_SMS_ON_NOTIFY)
    public Mono<Void> sendSMSOnNotify(HttpEntity<String> requestEntity) {
        return patientSMSNotifyResponseOrchestrator.processResponse(requestEntity, X_HIP_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_PATIENTS_SMS_ON_NOTIFY));
    }
}
