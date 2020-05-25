package in.projecteka.gateway.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "gateway.serviceoptions")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class ServiceOptions {
    public int timeout;
    public String registryPath;
    public int responseMaxRetryAttempts;
}
