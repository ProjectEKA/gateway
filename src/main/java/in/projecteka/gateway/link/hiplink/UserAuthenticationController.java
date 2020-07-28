package in.projecteka.gateway.link.hiplink;

import in.projecteka.gateway.clients.UserAuthenticatorClient;
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
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_INIT;
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_ON_INIT;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RestController
@AllArgsConstructor
public class UserAuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthenticationController.class);
    RequestOrchestrator<UserAuthenticatorClient> userAuthenticationRequestOrchestrator;
    ResponseOrchestrator userAuthenticationResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_USERS_AUTH_INIT)
    public Mono<Void> authenticateUser(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        userAuthenticationRequestOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIP_ID, clientId)
                                .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_INIT)));

    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_USERS_AUTH_ON_INIT)
    public Mono<Void> onAuthenticateUser(HttpEntity<String> requestEntity) {
        logger.debug("Request from cm: {}", keyValue("users auth response", requestEntity.getBody()));
        return userAuthenticationResponseOrchestrator.processResponse(requestEntity, X_HIP_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_ON_INIT));
    }
}
