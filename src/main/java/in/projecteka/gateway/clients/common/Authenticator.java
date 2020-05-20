package in.projecteka.gateway.clients.common;

import in.projecteka.gateway.clients.Caller;
import reactor.core.publisher.Mono;

public interface Authenticator {
    Mono<Caller> verify(String token);
}