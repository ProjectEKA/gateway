package in.projecteka.gateway.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BridgeRegistry {
    @Autowired
    YamlRegistry yamlRegistry;

    public Optional<YamlRegistryMapping> getConfigFor(String id, ServiceType serviceType) {
        return yamlRegistry.getBridges().stream()
                .filter(yamlRegistryMapping -> yamlRegistryMapping.getId().equals(id))
                .filter(yamlRegistryMapping -> yamlRegistryMapping.getServesAs().contains(serviceType))
                .findFirst();
    }
}
