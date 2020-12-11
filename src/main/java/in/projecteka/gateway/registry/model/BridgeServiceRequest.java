package in.projecteka.gateway.registry.model;

import in.projecteka.gateway.registry.ServiceType;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Value
@Builder
@NotNull
public class BridgeServiceRequest {
    @NotEmpty(message = "Service id should be specified")
    String id;
    String name;
    ServiceType type;
    boolean active;
}