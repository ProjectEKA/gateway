package in.projecteka.gateway.registry.model;

import in.projecteka.gateway.registry.ServiceType;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Value
@Builder
@NotNull
public class BridgeServiceRequest {
    @NotEmpty(message = "Service id should be specified")
    String id;
    String name;
    ServiceType type;
    List<EndpointDetails> endpoints;
    boolean active;
}