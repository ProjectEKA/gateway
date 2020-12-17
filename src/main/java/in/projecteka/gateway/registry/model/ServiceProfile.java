package in.projecteka.gateway.registry.model;

import in.projecteka.gateway.registry.ServiceType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ServiceProfile {
    String id;
    String name;
    List<ServiceType> types;
    Endpoints endpoints;
    boolean active;
}
