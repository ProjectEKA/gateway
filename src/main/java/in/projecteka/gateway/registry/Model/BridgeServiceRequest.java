package in.projecteka.gateway.registry.Model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BridgeServiceRequest {
    private String bridgeId;
    private List<Service> services;
}