package in.projecteka.gateway.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class GatewayResponse {
    private UUID requestId;
}
