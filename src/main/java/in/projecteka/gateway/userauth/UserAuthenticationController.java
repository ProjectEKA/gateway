package in.projecteka.gateway.userauth;

import in.projecteka.gateway.clients.AuthNotifyServiceClient;
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

import java.util.Objects;

import static in.projecteka.gateway.common.Constants.API_CALLED;
import static in.projecteka.gateway.common.Constants.PATH_CONSENT_REQUESTS_INIT;
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_INIT;
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_NOTIFY;
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_ON_INIT;
import static in.projecteka.gateway.common.Constants.PATH_USERS_AUTH_ON_NOTIFY;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Constants.bridgeId;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RestController
@AllArgsConstructor
public class UserAuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthenticationController.class);
    RequestOrchestrator<UserAuthenticatorClient> userAuthenticationRequestOrchestrator;
    ResponseOrchestrator userAuthenticationResponseOrchestrator;
    RequestOrchestrator<AuthNotifyServiceClient> authNotifyRequestOrchestrator;
    ResponseOrchestrator authNotifyResponseOrchestrator;


    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_USERS_AUTH_INIT)
    public Mono<Void> authenticateUser(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> {
                    if (isRequestFromHIU(requestEntity))
                        return userAuthenticationRequestOrchestrator
                                .handleThis(requestEntity, X_CM_ID, X_HIU_ID, bridgeId(clientId))
                                .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_INIT));
                    else
                        return userAuthenticationRequestOrchestrator
                                .handleThis(requestEntity, X_CM_ID, X_HIP_ID, bridgeId(clientId))
                                .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_INIT));
                });
    }

    private boolean isRequestFromHIU(HttpEntity<String> requestEntity) {
        return requestEntity.hasBody() && Objects.requireNonNull(requestEntity.getBody())
                .replaceAll("\\s+", "")
                .toLowerCase()
                .contains("\"requester\":{\"type\":\"hiu\",");
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_USERS_AUTH_ON_INIT)
    public Mono<Void> onAuthenticateUser(HttpEntity<String> requestEntity) {
        logger.debug("Request from cm: {}", keyValue("users auth response", requestEntity.getBody()));
        if (requestEntity.getHeaders().containsKey(X_HIU_ID))
            return userAuthenticationResponseOrchestrator.processResponse(requestEntity, X_HIU_ID)
                    .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_ON_INIT));
        else
            return userAuthenticationResponseOrchestrator.processResponse(requestEntity, X_HIP_ID)
                    .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_ON_INIT));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_USERS_AUTH_NOTIFY)
    public Mono<Void> userAuthNotify(HttpEntity<String> requestEntity) {
        logger.info("Request from cm: users auth notify");
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> authNotifyRequestOrchestrator
                        .handleThis(requestEntity, getTargetService(requestEntity), X_CM_ID, clientId)
                        .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_NOTIFY)));
    }

    private String getTargetService(HttpEntity<String> requestEntity) {
        return requestEntity.getHeaders().containsKey(X_HIP_ID) ? X_HIP_ID : X_HIU_ID;
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_USERS_AUTH_ON_NOTIFY)
    public Mono<Void> authOnNotify(HttpEntity<String> requestEntity) {
        logger.info("Request from cm: users auth on-notify");
        return authNotifyResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_USERS_AUTH_ON_NOTIFY));
    }
}
