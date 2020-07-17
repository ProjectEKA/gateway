package in.projecteka.gateway.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeyCloakError {
    String error;
    @JsonProperty("error_description")
    String errorDescription;
    String errorMessage;
}
