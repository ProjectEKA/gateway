package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Endpoint {
    String use;
    String connectionType;
    String address;
}
