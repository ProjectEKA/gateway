package in.projecteka.gateway.session;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.clients.model.Session;
import in.projecteka.gateway.common.IdentityService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.V_1_CERTS;
import static in.projecteka.gateway.common.Constants.V_1_SESSIONS;
import static in.projecteka.gateway.common.Constants.V_1_WELL_KNOWN_OPENID_CONFIGURATION;

@RestController
@AllArgsConstructor
public class SessionController {
    private final IdentityService identityService;
    private final IdentityProperties centralRegistryProperties;

    @PostMapping(V_1_SESSIONS)
    public Mono<Session> with(@RequestBody SessionRequest session) {
        return identityService.getTokenFor(session.getClientId(), session.getClientSecret());
    }

    @GetMapping(V_1_WELL_KNOWN_OPENID_CONFIGURATION)
    public Mono<JsonNode> configuration() {
        return identityService.configuration(centralRegistryProperties.getHost());
    }

    @GetMapping(V_1_CERTS)
    public Mono<JsonNode> certs() {
        return identityService.certs();
    }
}
