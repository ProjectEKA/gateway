package in.projecteka.gateway.link.link;

import in.projecteka.gateway.clients.HipInitLinkServiceClient;
import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
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

import static in.projecteka.gateway.common.Constants.BRIDGE_ID_PREFIX;
import static in.projecteka.gateway.common.Constants.PATH_LINK_CONFIRM;
import static in.projecteka.gateway.common.Constants.PATH_LINK_INIT;
import static in.projecteka.gateway.common.Constants.PATH_LINK_ON_CONFIRM;
import static in.projecteka.gateway.common.Constants.PATH_LINK_ON_INIT;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.API_CALLED;
import static in.projecteka.gateway.common.Constants.PATH_ADD_CARE_CONTEXTS;
import static in.projecteka.gateway.common.Constants.PATH_ON_ADD_CARE_CONTEXTS;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RestController
@AllArgsConstructor
public class LinkController {
    private static final Logger logger = LoggerFactory.getLogger(LinkController.class);
    RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator;
    RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator;
    RequestOrchestrator<HipInitLinkServiceClient> hipInitLinkRequestOrchestrator;
    ResponseOrchestrator linkInitResponseOrchestrator;
    ResponseOrchestrator linkConfirmResponseOrchestrator;
    ResponseOrchestrator hipInitLinkResponseOrchestrator;


    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_LINK_INIT)
    public Mono<Void> linkInit(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        linkInitRequestOrchestrator.handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId))
                .subscriberContext(context -> context.put(API_CALLED, PATH_LINK_INIT));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_LINK_ON_INIT)
    public Mono<Void> linkOnInit(HttpEntity<String> requestEntity) {
        return linkInitResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_LINK_ON_INIT));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_LINK_CONFIRM)
    public Mono<Void> linkConfirm(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        linkConfirmRequestOrchestrator.handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId))
                .subscriberContext(context -> context.put(API_CALLED, PATH_LINK_CONFIRM));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_LINK_ON_CONFIRM)
    public Mono<Void> linkOnConfirm(HttpEntity<String> requestEntity) {
        return linkConfirmResponseOrchestrator.processResponse(requestEntity, X_CM_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_LINK_ON_CONFIRM));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_ADD_CARE_CONTEXTS)
    public Mono<Void> addCareContexts(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        hipInitLinkRequestOrchestrator
                                .handleThis(requestEntity, X_CM_ID, X_HIP_ID, BRIDGE_ID_PREFIX + clientId)
                                .subscriberContext(context -> context.put(API_CALLED, PATH_ADD_CARE_CONTEXTS)));

    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_ON_ADD_CARE_CONTEXTS)
    public Mono<Void> onAddCareContexts(HttpEntity<String> requestEntity) {
        logger.debug("Request from cm: {}", keyValue("Add Care context response", requestEntity.getBody()));
        return hipInitLinkResponseOrchestrator.processResponse(requestEntity, X_HIP_ID)
                .subscriberContext(context -> context.put(API_CALLED, PATH_ON_ADD_CARE_CONTEXTS));
    }
}
