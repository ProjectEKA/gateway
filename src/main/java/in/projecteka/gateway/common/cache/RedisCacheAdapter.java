package in.projecteka.gateway.common.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class RedisCacheAdapter implements CacheAdapter<String, String> {

    private final RedisClient redisClient;
    private StatefulRedisConnection<String, String> statefulConnection;
    private int expirationInMinutes;

    public RedisCacheAdapter(RedisClient redisClient, int expirationInMinutes) {
        this.redisClient = redisClient;
        this.expirationInMinutes = expirationInMinutes;
    }

    @PostConstruct
    public void postConstruct() {
        statefulConnection = redisClient.connect();
    }

    @PreDestroy
    public void preDestroy() {
        statefulConnection.close();
        redisClient.shutdown();
    }

    @Override
    public Mono<String> get(String key) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.get(key);
    }

    @Override
    public Mono<Void> put(String key, String value) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.set(key, value)
                .then(redisCommands.expire(key, expirationInMinutes * 60L))
                .then();
    }

    @Override
    public Mono<Void> invalidate(String key) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return redisCommands.expire(key, 0L).then();
    }

}
