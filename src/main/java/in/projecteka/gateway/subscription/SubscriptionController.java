package in.projecteka.gateway.subscription;

import in.projecteka.gateway.clients.SubscriptionRequestServiceClient;
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
import static in.projecteka.gateway.common.Constants.PATH_SUBSCRIPTION_REQUESTS_INIT;
import static in.projecteka.gateway.common.Constants.PATH_SUBSCRIPTION_REQUESTS_ON_INIT;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Constants.bridgeId;

@RestController
@AllArgsConstructor
public class SubscriptionController {
    RequestOrchestrator<SubscriptionRequestServiceClient> subscriptionRequestOrchestrator;
    ResponseOrchestrator subscriptionResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_SUBSCRIPTION_REQUESTS_INIT)
    public Mono<Void> createSubscriptionRequest(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> subscriptionRequestOrchestrator
                        .handleThis(requestEntity, X_CM_ID, X_HIU_ID, bridgeId(clientId))
                        .subscriberContext(context -> context.put(API_CALLED, PATH_SUBSCRIPTION_REQUESTS_INIT)));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_SUBSCRIPTION_REQUESTS_ON_INIT)
    public Mono<Void> onCreateSubscriptionRequest(HttpEntity<String> requestEntity) {
        return subscriptionResponseOrchestrator.processResponse(requestEntity, X_HIU_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_SUBSCRIPTION_REQUESTS_ON_INIT));
    }
}
