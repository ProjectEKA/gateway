package in.projecteka.gateway.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Builder
@Data
@AllArgsConstructor
public class CmErrorResponse {
    private Error error;
}
