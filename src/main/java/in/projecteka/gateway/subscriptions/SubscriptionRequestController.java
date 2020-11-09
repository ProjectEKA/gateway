package in.projecteka.gateway.subscriptions;

import in.projecteka.gateway.clients.SubscriptionRequestNotifyServiceClient;
import in.projecteka.gateway.clients.SubscriptionRequestServiceClient;
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
import static in.projecteka.gateway.common.Constants.PATH_SUBSCRIPTION_REQUESTS_INIT_ON_GW;
import static in.projecteka.gateway.common.Constants.PATH_SUBSCRIPTION_REQUESTS_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_SUBSCRIPTION_REQUESTS_ON_INIT_ON_GW;
import static in.projecteka.gateway.common.Constants.PATH_SUBSCRIPTION_REQUESTS_ON_NOTIFY;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Constants.bridgeId;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RestController
@AllArgsConstructor
public class SubscriptionRequestController {
    RequestOrchestrator<SubscriptionRequestServiceClient> subscriptionRequestOrchestrator;
    ResponseOrchestrator subscriptionResponseOrchestrator;
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionRequestController.class);
    RequestOrchestrator<SubscriptionRequestNotifyServiceClient> subscriptionRequestNotifyOrchestrator;
    ResponseOrchestrator subscriptionRequestNotifyResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_SUBSCRIPTION_REQUESTS_INIT_ON_GW)
    public Mono<Void> createSubscriptionRequest(HttpEntity<String> requestEntity) {
        logger.debug("Request from hiu: {}", keyValue("Subscription Init", requestEntity.getBody()));
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> subscriptionRequestOrchestrator
                        .handleThis(requestEntity, X_CM_ID, X_HIU_ID, bridgeId(clientId))
                        .subscriberContext(context -> context.put(API_CALLED, PATH_SUBSCRIPTION_REQUESTS_INIT_ON_GW)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_SUBSCRIPTION_REQUESTS_ON_INIT_ON_GW)
    public Mono<Void> onCreateSubscriptionRequest(HttpEntity<String> requestEntity) {
        logger.debug("Response from CM: {}", keyValue("Subscription On Init", requestEntity.getBody()));
        return subscriptionResponseOrchestrator.processResponse(requestEntity, X_HIU_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_SUBSCRIPTION_REQUESTS_ON_INIT_ON_GW));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_SUBSCRIPTION_REQUESTS_NOTIFY)
    public Mono<Void> notifySubscriptionRequestToHIU(HttpEntity<String> requestEntity) {
        logger.debug("Request from cm: {}", keyValue("hiu subscription request notification", requestEntity.getBody()));
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> subscriptionRequestNotifyOrchestrator
                        .handleThis(requestEntity, X_HIU_ID, X_CM_ID, clientId))
                .subscriberContext(context -> context.put(API_CALLED, PATH_HIU_SUBSCRIPTION_NOTIFY));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_SUBSCRIPTION_REQUESTS_ON_NOTIFY)
    public Mono<Void> onNotifySubscriptionRequestToCM(HttpEntity<String> requestEntity) {
        logger.debug("Response from hiu: {}", keyValue("hiu subscription request on notify", requestEntity.getBody()));
        return subscriptionRequestNotifyResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_HIU_SUBSCRIPTION_ON_NOTIFY));
    }

}
