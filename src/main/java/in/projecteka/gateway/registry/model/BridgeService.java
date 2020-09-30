package in.projecteka.gateway.registry.model;


import in.projecteka.gateway.registry.ServiceType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BridgeService {
    String id;
    ServiceType type;
}
