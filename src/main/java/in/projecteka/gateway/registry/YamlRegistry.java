package in.projecteka.gateway.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class YamlRegistry {
    List<YamlRegistryMapping> consentManagers;
    List<YamlRegistryMapping> bridges;
}
