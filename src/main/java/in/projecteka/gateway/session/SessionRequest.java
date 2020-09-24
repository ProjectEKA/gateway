package in.projecteka.gateway.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.gateway.common.model.GrantType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionRequest {
    String clientId;
    String clientSecret;
    String refreshToken;
    GrantType grantType;
}
