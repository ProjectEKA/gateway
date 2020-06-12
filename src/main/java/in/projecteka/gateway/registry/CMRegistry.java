package in.projecteka.gateway.registry;

import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class CMRegistry {
    YamlRegistry yamlRegistry;

    public Optional<YamlRegistryMapping> getConfigFor(String id) {
        return yamlRegistry.getConsentManagers()
                .stream()
                .filter(yamlRegistryMapping -> yamlRegistryMapping.getId().equals(id))
                .findFirst();
    }
}
