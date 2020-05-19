package in.projecteka.gateway.link.discovery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class DiscoveryController {
    @Autowired
    DiscoveryHelper discoveryHelper;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/patients/care-contexts/discover")
    public Mono<Void> discoverCareContext(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = discoveryHelper.doDiscoverCareContext(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/patients/care-contexts/on-discover")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        Mono<Void> tobeFiredAndForgotten = discoveryHelper.doOnDiscoverCareContext(requestEntity);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }
}
