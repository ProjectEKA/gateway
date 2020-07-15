package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;

@AllArgsConstructor
public class RegistryService {
    private final RegistryRepository registryRepository;
    private final CacheAdapter<String, String> consentManagerMappings;

    public Mono<Void> populateCMEntry(CMServiceRequest cmServiceRequest) {
        return registryRepository.getCMEntryCount(cmServiceRequest.getCmSuffix())
                .flatMap(count ->
                        count > 0
                                ? registryRepository.updateCMEntry(cmServiceRequest)
                                .then(consentManagerMappings.invalidate(cmServiceRequest.getCmSuffix()))
                                : registryRepository.createCMEntry(cmServiceRequest));
    }
}
