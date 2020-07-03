package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class CMRegistry {
    private final CacheAdapter<String, String> consentManagerMappings;

    public Optional<String> getHostFor(String id) {
        return Optional.ofNullable(consentManagerMappings.get(id).toString());
    }
}
