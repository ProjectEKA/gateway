package in.projecteka.gateway.link.link;

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
    LinkHelper linkHelper;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/links/link/init")
    public Mono<Void> linkInit(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/v1/links/link/on-init")
    public Mono<Void> linkOnInit(HttpEntity<String> requestEntity) {
        Mono<Void> toBeFiredAndForgotten = linkHelper.doOnLinkInit(requestEntity);
        toBeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
