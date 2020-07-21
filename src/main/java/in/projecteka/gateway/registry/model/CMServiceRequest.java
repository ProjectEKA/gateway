package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CMServiceRequest {
    String name;
    String url;
    String suffix;
    Boolean isActive;
    Boolean isBlocklisted;
}
