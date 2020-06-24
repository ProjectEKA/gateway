package in.projecteka.gateway.clients.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {

    @JsonAlias({"access_token"})
    String accessToken;

    @JsonAlias({"expires_in"})
    int expiresIn;

    @JsonAlias({"refresh_expires_in"})
    int refreshExpiresIn;

    @JsonAlias({"refresh_token"})
    String refreshToken;

    @JsonAlias({"token_type"})
    String tokenType;
}
