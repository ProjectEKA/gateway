package in.projecteka.gateway.link.link;

import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class ValidatedLinkInitRequest {
    private final YamlRegistryMapping hipConfig;
    private final String cmRequestId;
    private final Map<String, Object> deserializedRequest;
}
