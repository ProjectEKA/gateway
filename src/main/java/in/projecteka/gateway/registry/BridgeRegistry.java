package in.projecteka.gateway.registry;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

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

    public Mono<String> getHostFor(String id, ServiceType serviceType) {
        //return getConfigFor(id, serviceType).map(YamlRegistryMapping::getHost);
        return Mono.just("test");
    }
}
