package in.projecteka.gateway.session;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.IdentityProperties;
import in.projecteka.gateway.clients.model.Session;
import in.projecteka.gateway.common.IdentityService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.PATH_CERTS;
import static in.projecteka.gateway.common.Constants.PATH_SESSIONS;
import static in.projecteka.gateway.common.Constants.PATH_WELL_KNOWN_OPENID_CONFIGURATION;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RestController
@AllArgsConstructor
public class SessionController {
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    private final IdentityService identityService;
    private final IdentityProperties centralRegistryProperties;

    @PostMapping(PATH_SESSIONS)
    public Mono<Session> with(@RequestBody SessionRequest session) {
        logger.info("Session request received {}", keyValue("clientId", session.getClientId()));
        return identityService.getTokenFor(session.getClientId(), session.getClientSecret());
    }

    @GetMapping(PATH_WELL_KNOWN_OPENID_CONFIGURATION)
    public Mono<JsonNode> configuration() {
        return identityService.configuration(centralRegistryProperties.getHost());
    }

    @GetMapping(PATH_CERTS)
    public Mono<JsonNode> certs() {
        return identityService.certs();
    }
}
