package in.projecteka.gateway.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@Component
public class YamlRegistrySource {
    @Autowired
    YamlRegistry yamlRegistry;

//    @PostConstruct
//    public void init() {
//        System.out.println(yamlRegistry.getConsentManagers());
//        System.out.println(yamlRegistry.getHip());
//    }

    public Optional<YamlRegistryMapping> getConfigFor(ServiceType serviceType, String id) {
        Predicate<YamlRegistryMapping> matchingPredicate = yamlRegistryMapping -> yamlRegistryMapping.getId().equals(id);
        switch (serviceType) {
            case CONSENT_MANAGER:
                return yamlRegistry.getConsentManagers().stream().filter(matchingPredicate).findFirst();
            case HIP:
                return yamlRegistry.getHip().stream().filter(matchingPredicate).findFirst();
            default:
                return Optional.empty();
        }
    }
}
