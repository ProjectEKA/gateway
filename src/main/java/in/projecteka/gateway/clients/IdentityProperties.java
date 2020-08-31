package in.projecteka.gateway.clients;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import static java.lang.String.format;

@ConfigurationProperties(prefix = "identity")
@AllArgsConstructor
@Getter
@ConstructorBinding
public class IdentityProperties {
    private final String url;
    private final String clientSecret;
    private final String clientId;
    private final String host;
    private final String realm;
    private final String userName;
    private final String password;
    private final int accessTokenExpiryInMinutes;

    public String getJwkUrl() {
        return format("%s/realms/%s/protocol/openid-connect/certs", url, realm);
    }
}
