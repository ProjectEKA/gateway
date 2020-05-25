package in.projecteka.gateway.link.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidatedResponse {
    String xCmId;
    String callerRequestId;
    JsonNode deserializedJsonNode;
}
