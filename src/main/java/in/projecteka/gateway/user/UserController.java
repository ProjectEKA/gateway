package in.projecteka.gateway.user;

import in.projecteka.gateway.clients.AuthConfirmServiceClient;
import in.projecteka.gateway.clients.AuthModeFetchClient;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
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

import java.util.Objects;

import static in.projecteka.gateway.common.Constants.API_CALLED;
import static in.projecteka.gateway.common.Constants.PATH_FETCH_AUTH_MODES;
import static in.projecteka.gateway.common.Constants.PATH_ON_FETCH_AUTH_MODES;
import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_FIND;
import static in.projecteka.gateway.common.Constants.PATH_PATIENTS_ON_FIND;
import static in.projecteka.gateway.common.Constants.USERS_AUTH_CONFIRM;
import static in.projecteka.gateway.common.Constants.USERS_AUTH_ON_CONFIRM;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Constants.bridgeId;


@RestController
@AllArgsConstructor
public class UserController {
    RequestOrchestrator<PatientSearchServiceClient> patientSearchRequestOrchestrator;
    ResponseOrchestrator patientSearchResponseOrchestrator;
    RequestOrchestrator<AuthConfirmServiceClient> authConfirmRequestOrchestrator;
    ResponseOrchestrator authConfirmResponseOrchestrator;
    RequestOrchestrator<AuthModeFetchClient> authModeFetchRequestOrchestrator;
    ResponseOrchestrator authModeFetchResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENTS_FIND)
    public Mono<Void> findPatient(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> patientSearchRequestOrchestrator
                        .handleThis(requestEntity, X_CM_ID, X_HIU_ID, bridgeId(clientId))
                        .subscriberContext(context -> context.put(API_CALLED, PATH_PATIENTS_FIND)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_PATIENTS_ON_FIND)
    public Mono<Void> onFindPatient(HttpEntity<String> requestEntity) {
        return patientSearchResponseOrchestrator.processResponse(requestEntity, X_HIU_ID)
                .subscriberContext(context -> context.put("apiCalled", PATH_PATIENTS_ON_FIND));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(USERS_AUTH_CONFIRM)
    public Mono<Void> authConfirm(HttpEntity<String> requestEntity){
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> authConfirmRequestOrchestrator
                        .handleThis(requestEntity, X_CM_ID, X_HIP_ID, bridgeId(clientId))
                        .subscriberContext(context -> context.put(API_CALLED, USERS_AUTH_CONFIRM)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(USERS_AUTH_ON_CONFIRM)
    public Mono<Void> authOnConfirm(HttpEntity<String> requestEntity){
        return authConfirmResponseOrchestrator.processResponse(requestEntity, X_HIP_ID)
                .subscriberContext(context -> context.put(API_CALLED, USERS_AUTH_ON_CONFIRM));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_FETCH_AUTH_MODES)
    public Mono<Void> fetchAuthModes(HttpEntity<String> requestEntity){
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> {
                    if (isRequestFromHIU(requestEntity))
                        return authModeFetchRequestOrchestrator
                                .handleThis(requestEntity, X_CM_ID, X_HIU_ID, bridgeId(clientId))
                                .subscriberContext(context -> context.put(API_CALLED, PATH_FETCH_AUTH_MODES));
                    else
                        return authModeFetchRequestOrchestrator
                                .handleThis(requestEntity, X_CM_ID, X_HIP_ID, bridgeId(clientId))
                                .subscriberContext(context -> context.put(API_CALLED, PATH_FETCH_AUTH_MODES));
                });
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_ON_FETCH_AUTH_MODES)
    public Mono<Void> onFetchAuthModesHIP(HttpEntity<String> requestEntity){
        return authModeFetchResponseOrchestrator.processResponse(requestEntity, getTargetService(requestEntity))
                .subscriberContext(context -> context.put(API_CALLED, PATH_ON_FETCH_AUTH_MODES));
    }


    private boolean isRequestFromHIU(HttpEntity<String> requestEntity) {
        return requestEntity.hasBody() && Objects.requireNonNull(requestEntity.getBody())
                .replaceAll("\\s+", "")
                .toLowerCase()
                .contains("\"requester\":{\"type\":\"hiu\",");
    }

    private String getTargetService(HttpEntity<String> requestEntity){
        return requestEntity.getHeaders().containsKey(X_HIP_ID) ? X_HIP_ID : X_HIU_ID;
    }
}
