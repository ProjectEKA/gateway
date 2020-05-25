package in.projecteka.gateway.link.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidatedResponse {
    YamlRegistryMapping cmConfig;
    String callerRequestId;
    JsonNode deserializedJsonNode;
}
