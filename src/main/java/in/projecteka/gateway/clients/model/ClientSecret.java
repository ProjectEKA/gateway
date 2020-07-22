package in.projecteka.gateway.clients.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClientSecret {
    String type;
    String value;
}
