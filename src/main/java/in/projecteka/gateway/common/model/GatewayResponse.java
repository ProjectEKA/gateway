package in.projecteka.gateway.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class GatewayResponse {
    private static final long serialVersionUID = 7_333_234_669_771_595_601L;
    private final UUID requestId;
}
