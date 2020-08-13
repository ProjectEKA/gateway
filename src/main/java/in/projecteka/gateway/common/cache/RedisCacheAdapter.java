package in.projecteka.gateway.common.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;

import static reactor.core.publisher.Mono.defer;

public class RedisCacheAdapter implements CacheAdapter<String, String> {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheAdapter.class);
    public static final String RETRIED_AT = "retried at {}";

    private final RedisClient redisClient;
    private StatefulRedisConnection<String, String> statefulConnection;
    private final int expirationInMinutes;
    private final int retry;

    public RedisCacheAdapter(RedisClient redisClient, int expirationInMinutes, int retry) {
        this.redisClient = redisClient;
        this.expirationInMinutes = expirationInMinutes;
        this.retry = retry;
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
        return retryable(redisCommands.get(key));
    }

    @Override
    public Mono<Void> put(String key, String value) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        Mono<Void> putOperation = redisCommands.set(key, value)
                .then(redisCommands.expire(key, expirationInMinutes * 60L))
                .then();
        return retryable(putOperation);
    }

    @Override
    public Mono<Void> invalidate(String key) {
        RedisReactiveCommands<String, String> redisCommands = statefulConnection.reactive();
        return retryable(redisCommands.expire(key, 0).then());
    }

    private <U> Mono<U> retryable(Mono<U> producer) {
        return defer(() -> producer)
                .doOnError(error -> logger.error(error.getMessage(), error))
                .retryWhen(Retry
                        .backoff(retry, Duration.ofMillis(100)).jitter(0d)
                        .doAfterRetry(rs -> logger.error(RETRIED_AT, LocalDateTime.now()))
                        .onRetryExhaustedThrow((spec, rs) -> rs.failure()));
    }
}
