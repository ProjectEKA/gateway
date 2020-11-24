package in.projecteka.gateway.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class HFRFacilityRepresentation {
    private String id;
    private String name;
    private String contactNumber;
    private Address address;

    @AllArgsConstructor
    @Builder
    @Data
    public static class Address {
        private String city;
    }
}
