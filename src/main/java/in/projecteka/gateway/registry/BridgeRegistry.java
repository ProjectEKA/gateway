package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Constants.BRIDGE_ID_PREFIX;

@AllArgsConstructor
public class BridgeRegistry {
    private final CacheAdapter<String, String> bridgeMappings;
    private final MappingRepository mappingRepository;

    public Mono<String> getHostFor(String id, ServiceType serviceType) {
        return bridgeMappings.get(bridgeMappingKey(id, serviceType))
                .switchIfEmpty((id.startsWith(BRIDGE_ID_PREFIX)
                        ? mappingRepository.bridgeHost(id.substring(BRIDGE_ID_PREFIX.length()))
                        : mappingRepository.bridgeHost(bridgeMappingKey(id, serviceType)))
                        .filter(url -> StringUtils.hasText(url) && UrlUtils.isAbsoluteUrl(url))
                        .flatMap(url -> bridgeMappings.put(bridgeMappingKey(id, serviceType), url).thenReturn(url)));
    }

    private String bridgeMappingKey(String id, ServiceType serviceType) {
        return String.join("-", id, serviceType.name());
    }
}