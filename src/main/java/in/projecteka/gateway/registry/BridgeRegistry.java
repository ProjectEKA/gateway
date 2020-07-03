package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BridgeRegistry {
    private final CacheAdapter<String, String> bridgeMappings;
    // BridgeMappingKey bridgeMappingKey;

//    public Optional<YamlRegistryMapping> getConfigFor(String id, ServiceType serviceType) {
//        return yamlRegistry.getBridges().stream()
//                .filter(yamlRegistryMapping -> yamlRegistryMapping.getId().equals(id))
//                .filter(yamlRegistryMapping -> yamlRegistryMapping.getServesAs().contains(serviceType))
//                .findFirst();
//    }

//    public Optional<String> getHostFor(String id, ServiceType serviceType) {
//        return getConfigFor(id, serviceType).map(YamlRegistryMapping::getHost);
//        return Optional.ofNullable(bridgeMappings.get(bridgeMappingKey).toString());
//    }
//}
//
//
//    public Optional<String> getHostFor(String id) {
//        return Optional.ofNullable(consentManagerMappings.get(id).toString());
//    }
}