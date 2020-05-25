package in.projecteka.gateway.link.link;

import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
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
public class LinkController {
    RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator;
    ResponseOrchestrator<LinkInitServiceClient> linkInitResponseOrchestrator;
    RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/links/link/init")
    public Mono<Void> linkInit(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = linkInitRequestOrchestrator.processRequest(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/links/link/on-init")
    public Mono<Void> linkOnInit(HttpEntity<String> requestEntity) {
        Mono<Void> toBeFiredAndForgotten = linkInitResponseOrchestrator.processResponse(requestEntity);
        toBeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/links/link/confirm")
    public Mono<Void> linkConfirm(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = linkConfirmRequestOrchestrator.processRequest(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
