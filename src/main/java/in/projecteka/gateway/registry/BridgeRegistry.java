package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.BRIDGE_ID_PREFIX;

@AllArgsConstructor
public class BridgeRegistry {
    private final CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;
    private final MappingRepository mappingRepository;

    public Mono<String> getHostFor(String id, ServiceType serviceType) {
        return bridgeMappings.get(Pair.of(id, serviceType))
                .switchIfEmpty((id.startsWith(BRIDGE_ID_PREFIX)
                        ? mappingRepository.bridgeHost(id.substring(BRIDGE_ID_PREFIX.length()))
                        : mappingRepository.bridgeHost(Pair.of(id, serviceType)))
                        .filter(url -> StringUtils.hasText(url) && UrlUtils.isAbsoluteUrl(url))
                        .flatMap(url -> bridgeMappings.put(Pair.of(id, serviceType), url).thenReturn(url)));
    }
}