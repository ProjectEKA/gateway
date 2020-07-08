package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class CMRegistry {
    private final CacheAdapter<String, String> consentManagerMappings;
    private final MappingRepository mappingRepository;

    public Mono<String> getHostFor(String id) {
        return consentManagerMappings.get(id)
                .switchIfEmpty(mappingRepository.cmHost(id)
                        .filter(url -> StringUtils.hasText(url) && UrlUtils.isAbsoluteUrl(url))
                        .flatMap(url -> consentManagerMappings.put(id, url).thenReturn(url)));
    }
}
