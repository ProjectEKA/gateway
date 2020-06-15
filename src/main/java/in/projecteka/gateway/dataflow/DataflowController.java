package in.projecteka.gateway.dataflow;

import in.projecteka.gateway.clients.DataFlowRequestServiceClient;
import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.common.RequestOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.V_1_HEALTH_INFORMATION_CM_REQUEST;
import static in.projecteka.gateway.common.Constants.X_CM_ID;

@RestController
@AllArgsConstructor
public class DataflowController {
    RequestOrchestrator<DataFlowRequestServiceClient> dataflowRequestRequestOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(V_1_HEALTH_INFORMATION_CM_REQUEST)
    public Mono<Void> initDataflowRequest(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> dataflowRequestRequestOrchestrator.handleThis(requestEntity, X_CM_ID, clientId));
    }
}
