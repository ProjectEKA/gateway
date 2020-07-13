package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.Model.CMServiceRequest;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class RegistryService {
    private final RegistryRepository registryRepository;

    public Mono<Void> populateCMEntry(CMServiceRequest cmServiceRequest) {
        return registryRepository.upsertCMEntry(cmServiceRequest);
        //Create keycloak client for CM entry;
    }
}
