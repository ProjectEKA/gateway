package in.projecteka.gateway.link.link;

import in.projecteka.gateway.clients.LinkServiceClient;
import in.projecteka.gateway.link.discovery.Orchestrator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class LinkController {
    Orchestrator<LinkServiceClient> linkOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/links/link/init")
    public Mono<Void> linkInit(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = linkOrchestrator.processRequest(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/links/link/on-init")
    public Mono<Void> linkOnInit(HttpEntity<String> requestEntity) {
        Mono<Void> toBeFiredAndForgotten = linkOrchestrator.processResponse(requestEntity);
        toBeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
