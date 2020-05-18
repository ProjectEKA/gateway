package in.projecteka.gateway.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@Component
public class CMRegistry {
    @Autowired
    YamlRegistry yamlRegistry;

    public Optional<YamlRegistryMapping> getConfigFor(String id) {
        return yamlRegistry.getConsentManagers().stream().filter(yamlRegistryMapping -> yamlRegistryMapping.getId().equals(id)).findFirst();
    }
}
