package in.projecteka.gateway.common;

import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class ValidatedRequest {
    private final YamlRegistryMapping config;
    private final String requesterRequestId;
    private final Map<String, Object> deserializedRequest;
}
