package in.projecteka.gateway.registry.Model;

import lombok.Value;

@Value
public class ClientCredentials {
    String clientKey;
    String clientSecret;
}
