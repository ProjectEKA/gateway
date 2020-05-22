package in.projecteka.gateway.link.discovery;

import in.projecteka.gateway.clients.DiscoveryServiceClient;
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
    Orchestrator<DiscoveryServiceClient> discoveryOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/care-contexts/discover")
    public Mono<Void> discoverCareContext(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = discoveryOrchestrator.processRequest(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/care-contexts/on-discover")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = discoveryOrchestrator.processResponse(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
