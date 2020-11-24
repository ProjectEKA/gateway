package in.projecteka.gateway.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;


@ConfigurationProperties(prefix = "facility-registry")
@AllArgsConstructor
@Getter
@ConstructorBinding
public class FacilityRegistryProperties {
    private final String url;
    private final String clientSecret;
    private final String clientId;
    private final String authUrl;
    private final int tokenExpiry;
}
