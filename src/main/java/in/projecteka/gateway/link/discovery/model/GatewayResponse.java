package in.projecteka.gateway.link.discovery.model;

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
