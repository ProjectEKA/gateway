package in.projecteka.gateway.registry;

import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class BridgeRegistry {
    YamlRegistry yamlRegistry;

    public Optional<YamlRegistryMapping> getConfigFor(String id, ServiceType serviceType) {
        return yamlRegistry.getBridges().stream()
                .filter(yamlRegistryMapping -> yamlRegistryMapping.getId().equals(id))
                .filter(yamlRegistryMapping -> yamlRegistryMapping.getServesAs().contains(serviceType))
                .findFirst();
    }
}
