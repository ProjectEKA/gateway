package in.projecteka.gateway.dataflow;

import in.projecteka.gateway.clients.DataFlowRequestServiceClient;
import in.projecteka.gateway.clients.HealthInfoNotificationServiceClient;
import in.projecteka.gateway.clients.HipDataFlowServiceClient;
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

import static in.projecteka.gateway.common.Constants.V_1_HEALTH_INFORMATION_CM_ON_REQUEST;
import static in.projecteka.gateway.common.Constants.V_1_HEALTH_INFORMATION_CM_REQUEST;
import static in.projecteka.gateway.common.Constants.V_1_HEALTH_INFORMATION_HIP_ON_REQUEST;
import static in.projecteka.gateway.common.Constants.V_1_HEALTH_INFORMATION_HIP_REQUEST;
import static in.projecteka.gateway.common.Constants.V_1_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;

@RestController
@AllArgsConstructor
public class DataflowController {
    RequestOrchestrator<DataFlowRequestServiceClient> dataflowRequestRequestOrchestrator;
    RequestOrchestrator<HipDataFlowServiceClient> hipDataflowRequestOrchestrator;
    ResponseOrchestrator hipDataFlowRequestResponseOrchestrator;
    ResponseOrchestrator dataFlowRequestResponseOrchestrator;
    RequestOrchestrator<HealthInfoNotificationServiceClient> healthInfoNotificationOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_HEALTH_INFORMATION_CM_REQUEST)
    public Mono<Void> initDataflowRequest(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        dataflowRequestRequestOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIU_ID, clientId));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_HEALTH_INFORMATION_CM_ON_REQUEST)
    public Mono<Void> onInitDataflowRequest(HttpEntity<String> requestEntity) {
        return dataFlowRequestResponseOrchestrator.processResponse(requestEntity, X_HIU_ID);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_HEALTH_INFORMATION_HIP_REQUEST)
    public Mono<Void> initHIPDataflowRequest(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId ->
                        hipDataflowRequestOrchestrator.handleThis(requestEntity, X_HIP_ID, X_CM_ID, clientId));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_HEALTH_INFORMATION_HIP_ON_REQUEST)
    public Mono<Void> hipDataFlowOnRequest(HttpEntity<String> requestEntity) {
        return hipDataFlowRequestResponseOrchestrator.processResponse(requestEntity, X_CM_ID);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_HEALTH_INFORMATION_NOTIFY)
    public Mono<Void> notifyToConsentManager(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> {
                    if(isRequestFromHIU(requestEntity))
                        return healthInfoNotificationOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIU_ID, clientId);
                    else
                        return healthInfoNotificationOrchestrator.handleThis(requestEntity, X_CM_ID, X_HIP_ID, clientId);
                });
    }

    private boolean isRequestFromHIU(HttpEntity<String> requestEntity) {
        return requestEntity.hasBody() && requestEntity.getBody().contains("\"notifier\":{\"type\":\"HIU\",");
    }
}
