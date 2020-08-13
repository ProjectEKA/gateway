package in.projecteka.gateway.common.cache;

import reactor.core.publisher.Mono;

public interface CacheAdapter<K, V> {
    Mono<V> get(K key);

    Mono<Void> put(K key, V value);

    Mono<Void> invalidate(K key);

    Mono<V> getIfPresent(K key);
}
