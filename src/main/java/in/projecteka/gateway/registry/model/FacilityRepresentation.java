package in.projecteka.gateway.registry.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class FacilityRepresentation {
    IdentifierRepresentation identifier;
    String city;
    String telephone;
    List<String> facilityType;
    Boolean isHIP;

    @AllArgsConstructor
    @Value
    @Builder
    public static class IdentifierRepresentation {
        String name;
        String id;
    }
}
