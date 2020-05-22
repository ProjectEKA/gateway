package in.projecteka.gateway.link.link;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidatedLinkInitResponse {
    YamlRegistryMapping cmConfig;
    String callerRequestId;
    JsonNode deserializedJsonNode;
}
