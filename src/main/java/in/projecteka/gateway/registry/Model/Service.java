package in.projecteka.gateway.registry.Model;

import in.projecteka.gateway.registry.ServiceType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Service {
    private String id;
    private ServiceType type;
    private boolean active;
}
