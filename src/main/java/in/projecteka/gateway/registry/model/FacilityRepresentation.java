package in.projecteka.gateway.registry.model;

import in.projecteka.gateway.registry.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class FacilityRepresentation {
    Identifier identifier;
    String city;
    String telephone;
    List<ServiceType> facilityType;
    Boolean isHIP;

    @AllArgsConstructor
    @Value
    @Builder
    public static class Identifier {
        String name;
        String id;
    }
}
