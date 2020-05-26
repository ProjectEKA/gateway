package in.projecteka.gateway.link.discovery;

import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.link.common.RequestOrchestrator;
import in.projecteka.gateway.link.common.ResponseOrchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class DiscoveryController {
    RequestOrchestrator<DiscoveryServiceClient> discoveryRequestOrchestrator;
    ResponseOrchestrator discoveryResponseOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/care-contexts/discover")
    public Mono<Void> discoverCareContext(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = discoveryRequestOrchestrator.processRequest(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/care-contexts/on-discover")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = discoveryResponseOrchestrator.processResponse(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
