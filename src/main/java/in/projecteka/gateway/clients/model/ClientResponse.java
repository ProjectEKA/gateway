package in.projecteka.gateway.clients.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClientResponse {
    String id;
    String secret;
}
