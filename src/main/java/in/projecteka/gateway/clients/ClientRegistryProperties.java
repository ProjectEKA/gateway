package in.projecteka.gateway.clients;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "clientregistry")
@AllArgsConstructor
@Getter
@ConstructorBinding
public class ClientRegistryProperties {
    private final String url;
    private final String clientSecret;
    private final String clientId;
    private final String jwkUrl;
}
