package in.projecteka.gateway.common.cache;

import lombok.Builder;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "gateway.serviceoptions")
@Value
@Builder
@ConstructorBinding
public class ServiceOptions {
    public final int timeout;
    public final String registryPath;
    public final int responseMaxRetryAttempts;
    public final int retryAttemptsDelay;
}
