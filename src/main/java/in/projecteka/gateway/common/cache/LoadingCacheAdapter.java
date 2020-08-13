package in.projecteka.gateway.common.cache;
import com.google.common.cache.LoadingCache;
import in.projecteka.gateway.exception.CacheNotAccessibleException;
import reactor.core.publisher.Mono;
import java.util.concurrent.ExecutionException;

public class LoadingCacheAdapter<K, V> implements CacheAdapter<K, V> {
    private final LoadingCache<K, V> loadingCache;
    public LoadingCacheAdapter(LoadingCache<K, V> loadingCache) {
        this.loadingCache = loadingCache;
    }
    @Override
    public Mono<V> get(K key) {
        try {
            V value = loadingCache.get(key);
            if (value != null && !value.equals("")) {
                return Mono.just(value);
            }
            return Mono.empty();
        } catch (ExecutionException e) {
            return Mono.error(new CacheNotAccessibleException("cache.not.accessible"));
        }
    }
    @Override
    public Mono<Void> put(K key, V value) {
        loadingCache.put(key, value);
        return Mono.empty();
    }

    @Override
    public Mono<Void> invalidate(K key) {
        loadingCache.invalidate(key);
        return Mono.empty();
    }

    @Override
    public Mono<V> getIfPresent(K key) {
        V value = loadingCache.getIfPresent(key);
        if (value != null && !value.equals("")) {
            return Mono.just(value);
        }
        return Mono.empty();
    }
}