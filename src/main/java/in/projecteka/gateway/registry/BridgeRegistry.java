package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class BridgeRegistry {
    private final CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;
    private final MappingRepository mappingRepository;

    public Mono<String> getHostFor(String id, ServiceType serviceType) {
        return bridgeMappings.get(Pair.of(id, serviceType))
                .switchIfEmpty(mappingRepository.bridgeHost(Pair.of(id, serviceType))
                    .flatMap(url -> bridgeMappings.put(Pair.of(id, serviceType), url)
                        .thenReturn(url)));
    }
}