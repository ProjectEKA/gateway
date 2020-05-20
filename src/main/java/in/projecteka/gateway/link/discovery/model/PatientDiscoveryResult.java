package in.projecteka.gateway.link.discovery.model;

import in.projecteka.gateway.clients.model.Error;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PatientDiscoveryResult {
    UUID requestId;
    UUID transactionId;
    Object patient;
    Error error;
    GatewayResponse resp;
}
