package in.projecteka.gateway.link.discovery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class GatewayResponse {
    private String requestId;
}
