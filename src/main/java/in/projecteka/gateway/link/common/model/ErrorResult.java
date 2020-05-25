package in.projecteka.gateway.link.common.model;

import in.projecteka.gateway.clients.model.Error;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
public class ErrorResult {
    @NotNull
    UUID requestId;
    Object link;
    Error error;
    @NotNull
    GatewayResponse resp;
}
