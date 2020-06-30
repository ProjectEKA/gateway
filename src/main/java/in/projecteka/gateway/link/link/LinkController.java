package in.projecteka.gateway.link.link;

import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
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

import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_CONFIRM;
import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_INIT;
import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_ON_CONFIRM;
import static in.projecteka.gateway.common.Constants.V_1_LINKS_LINK_ON_INIT;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Utils.requestInfoLog;
import static in.projecteka.gateway.common.Utils.responseInfoLog;

@RestController
@AllArgsConstructor
public class LinkController {
    private static final Logger logger = LoggerFactory.getLogger(LinkController.class);

    RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator;
    RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator;
    ResponseOrchestrator linkInitResponseOrchestrator;
    ResponseOrchestrator linkConfirmResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_LINKS_LINK_INIT)
    public Mono<Void> linkInit(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> {
                    requestInfoLog(requestEntity, clientId, X_CM_ID, X_HIP_ID, V_1_LINKS_LINK_INIT);
                    logger.info("Link init flow");

                    return linkInitRequestOrchestrator.handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId);
                });
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_LINKS_LINK_ON_INIT)
    public Mono<Void> linkOnInit(HttpEntity<String> requestEntity) {
        responseInfoLog(requestEntity, "HIP", X_CM_ID, V_1_LINKS_LINK_ON_INIT);
        logger.info("Link init flow");

        return linkInitResponseOrchestrator.processResponse(requestEntity, X_CM_ID);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_LINKS_LINK_CONFIRM)
    public Mono<Void> linkConfirm(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> {
                    requestInfoLog(requestEntity, clientId, X_CM_ID, X_HIP_ID, V_1_LINKS_LINK_CONFIRM);
                    logger.info("Link confirmation flow");

                    return linkConfirmRequestOrchestrator.handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId);
                });
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_LINKS_LINK_ON_CONFIRM)
    public Mono<Void> linkOnConfirm(HttpEntity<String> requestEntity) {
        responseInfoLog(requestEntity, "HIP", X_CM_ID, V_1_LINKS_LINK_ON_CONFIRM);
        logger.info("Link confirmation flow");

        return linkConfirmResponseOrchestrator.processResponse(requestEntity, X_CM_ID);
    }
}
