package in.projecteka.gateway.link.common.model;

import in.projecteka.gateway.clients.model.Error;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ErrorResult {
    UUID requestId;
    UUID transactionId;
    Object link;
    Error error;
    GatewayResponse resp;
}
