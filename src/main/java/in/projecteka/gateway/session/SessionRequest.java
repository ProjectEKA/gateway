package in.projecteka.gateway.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.gateway.common.model.GrantType;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionRequest {
    String clientId;
    String clientSecret;
    String refreshToken;
    @Valid
    @NotNull(message = "GrantType not specified.")
    GrantType grantType;
}
