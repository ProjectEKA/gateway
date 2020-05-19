package in.projecteka.gateway.link.discovery;

import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ValidatedDiscoverRequest {
    private final YamlRegistryMapping hipConfig;
}
