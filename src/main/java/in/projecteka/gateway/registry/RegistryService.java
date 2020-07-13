package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.Model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.Model.BridgeServiceRequest;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class RegistryService {
    private final RegistryRepository registryRepository;

    public Mono<Void> populateBridgeEntry(BridgeRegistryRequest bridgeRegistryRequest) {
        return registryRepository.upsertBridgeEntry(bridgeRegistryRequest);
    }

    public Mono<Void> populateBridgeServiceEntries(BridgeServiceRequest bridgeServiceRequest) {
        return registryRepository.upsertBridgeServiceEntries(bridgeServiceRequest);
    }
}
