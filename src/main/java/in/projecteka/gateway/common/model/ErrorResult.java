package in.projecteka.gateway.common.model;

import in.projecteka.gateway.clients.model.Error;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class ErrorResult extends Throwable {
    @NotNull
    private final UUID requestId;
    private final Object link;
    private final Error error;
    @NotNull
    private final GatewayResponse resp;
}
