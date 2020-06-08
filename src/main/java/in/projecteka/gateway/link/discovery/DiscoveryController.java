package in.projecteka.gateway.link.discovery;

import in.projecteka.gateway.clients.Caller;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
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

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;

@RestController
@AllArgsConstructor
public class DiscoveryController {
    RequestOrchestrator<DiscoveryServiceClient> discoveryRequestOrchestrator;
    ResponseOrchestrator discoveryResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/care-contexts/discover")
    public Mono<Void> discoverCareContext(HttpEntity<String> requestEntity) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getClientId)
                .flatMap(clientId -> discoveryRequestOrchestrator.processRequest(requestEntity, X_HIP_ID, clientId));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/care-contexts/on-discover")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        return discoveryResponseOrchestrator.processResponse(requestEntity, X_CM_ID);
    }
}
