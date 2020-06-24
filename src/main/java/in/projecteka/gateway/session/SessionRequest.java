package in.projecteka.gateway.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionRequest {
    String clientId;
    String clientSecret;
}
