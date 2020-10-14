package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ServiceProfileResponse {
    String id;
    String name;
    ServiceRole type;
    List<Endpoint> endpoints;
    boolean active;
}