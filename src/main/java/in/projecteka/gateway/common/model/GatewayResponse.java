package in.projecteka.gateway.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class GatewayResponse {
    private final UUID requestId;
}
