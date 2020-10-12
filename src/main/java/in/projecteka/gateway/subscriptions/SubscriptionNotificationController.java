package in.projecteka.gateway.subscriptions;

import in.projecteka.gateway.clients.HiuSubscriptionNotifyServiceClient;
import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.API_CALLED;
import static in.projecteka.gateway.common.Constants.PATH_HIU_SUBSCRIPTION_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_HIU_SUBSCRIPTION_ON_NOTIFY;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RestController
@AllArgsConstructor
public class SubscriptionNotificationController {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionNotificationController.class);

    RequestOrchestrator<HiuSubscriptionNotifyServiceClient> hiuSubscriptionNotifyRequestOrchestrator;
    ResponseOrchestrator hiuSubscriptionNotifyResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_HIU_SUBSCRIPTION_NOTIFY)
    public Mono<Void> notifySubscriptionToHIU(HttpEntity<String> requestEntity) {
        logger.debug("Request from cm: {}", keyValue("hiu subscription notification", requestEntity.getBody()));
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> hiuSubscriptionNotifyRequestOrchestrator
                        .handleThis(requestEntity, X_HIU_ID, X_CM_ID, clientId))
                .subscriberContext(context -> context.put(API_CALLED, PATH_HIU_SUBSCRIPTION_NOTIFY));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_HIU_SUBSCRIPTION_ON_NOTIFY)
    public Mono<Void> onNotifySubscriptionToHIU(HttpEntity<String> requestEntity) {
        return hiuSubscriptionNotifyResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_HIU_SUBSCRIPTION_ON_NOTIFY));
    }
}
