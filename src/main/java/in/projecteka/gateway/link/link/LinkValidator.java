package in.projecteka.gateway.link.link;

import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

public class LinkValidator {
    Mono<Void> validateLinkInit(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }

    Mono<Void> validateOnLinkInit(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }
}
