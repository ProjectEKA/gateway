package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.model.CMServiceRequest;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class RegistryService {
    private final RegistryRepository registryRepository;

    public Mono<Void> populateCMEntry(CMServiceRequest cmServiceRequest) {
        return registryRepository.getCMEntryCount(cmServiceRequest)
                .flatMap(count -> (count > 0) ? registryRepository.updateCMEntry(cmServiceRequest)
                        : registryRepository.createCMEntry(cmServiceRequest));
    }
}
