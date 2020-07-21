package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KeycloakClientSecret {
    String type;
    String value;
}
