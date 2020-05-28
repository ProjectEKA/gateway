package in.projecteka.gateway.common.model;

import in.projecteka.gateway.clients.model.Error;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
public class ErrorResult {
    @NotNull
    private final UUID requestId;
    private final Object link;
    private final Error error;
    @NotNull
    private final GatewayResponse resp;
}
