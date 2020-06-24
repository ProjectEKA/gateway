package in.projecteka.gateway.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class KeyCloakError {
    String error;
    @JsonProperty("error_description")
    String errorDescription;
}
