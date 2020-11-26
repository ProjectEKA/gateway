package in.projecteka.gateway.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
public class FacilitySearchResponse {
    private String referenceNumber;
    private List<HFRFacilityRepresentation> facilities;
}
