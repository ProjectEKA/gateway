package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;


@Builder
@Value
public class KeycloakClientCredentials {
    String key;
    String secret;
}
