package in.projecteka.gateway.link.link;

import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

public class LinkHelper {

    Mono<Void> doLinkInit(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }

    Mono<Void> doOnLinkInit(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }
}
