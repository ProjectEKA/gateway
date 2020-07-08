package in.projecteka.gateway.common.heartbeat.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "gateway.cachemethod")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class CacheMethodProperty {
    private final String methodName;
}