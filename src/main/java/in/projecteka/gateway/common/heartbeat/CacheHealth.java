package in.projecteka.gateway.common.heartbeat;

import io.lettuce.core.RedisClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;

@AllArgsConstructor
public class CacheHealth {
    private static final Logger logger = LoggerFactory.getLogger(CacheHealth.class);
    public static final String GUAVA = "guava";
    private final CacheMethodProperty cacheMethodProperty;
    private final RedisClient redisClient;

    public boolean isUp() {
        BooleanSupplier checkRedis = () -> {
            try (var ignored = redisClient.connect()) {
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        };
        return cacheMethodProperty.getMethodName().equals(GUAVA) || checkRedis.getAsBoolean();
    }
}
