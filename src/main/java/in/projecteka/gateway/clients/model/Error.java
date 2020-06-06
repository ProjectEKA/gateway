package in.projecteka.gateway.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class Error implements Serializable {
    private static final long serialVersionUID = 2_853_533_405_679_919_376L;

    private ErrorCode code;
    private String message;

    public static Error unKnownError(String message) {
        return new Error(ErrorCode.UNKNOWN_ERROR_OCCURRED, message);
    }
}


