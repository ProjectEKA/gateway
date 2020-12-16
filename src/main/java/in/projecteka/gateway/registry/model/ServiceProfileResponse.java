package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceProfileResponse {
    String id;
    String name;
    ServiceRole type;
    Endpoints endpoints;
    boolean active;
}