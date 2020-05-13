package in.projecteka.gateway.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties()
@AllArgsConstructor
@ConstructorBinding
@Getter
public class YamlRegistry {
    List<YamlRegistryMapping> consentManagers;
    List<YamlRegistryMapping> hip;
}
