package in.projecteka.gateway.data_notification;

import in.projecteka.gateway.clients.HipDataNotificationServiceClient;
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
import static in.projecteka.gateway.common.Constants.PATH_HIP_DATA_NOTIFICATION;
import static in.projecteka.gateway.common.Constants.PATH_HIP_DATA_NOTIFICATION_ACKNOWLEDGEMENT;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.bridgeId;


@RestController
@AllArgsConstructor
public class DataNotificationController {
    RequestOrchestrator<HipDataNotificationServiceClient> hipDataNotificationRequestOrchestrator;
    ResponseOrchestrator hipDataNotificationResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_HIP_DATA_NOTIFICATION)
    public Mono<Void> hipDataNotification(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> hipDataNotificationRequestOrchestrator
                        .handleThis(requestEntity, X_CM_ID, X_HIP_ID, bridgeId(clientId))
                        .subscriberContext(context -> context.put(API_CALLED, PATH_HIP_DATA_NOTIFICATION)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_HIP_DATA_NOTIFICATION_ACKNOWLEDGEMENT)
    public Mono<Void> hipDataNotificationAcknowledgement(HttpEntity<String> requestEntity) {
        return hipDataNotificationResponseOrchestrator.processResponse(requestEntity, X_HIP_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_HIP_DATA_NOTIFICATION_ACKNOWLEDGEMENT));
    }
}
