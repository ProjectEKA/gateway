package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ServiceDetailsResponse {
    String id;
    String name;
    ServiceRole type;
    List<EndpointDetails> endpoints;
    boolean active;
}
