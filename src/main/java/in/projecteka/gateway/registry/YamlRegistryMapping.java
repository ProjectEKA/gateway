package in.projecteka.gateway.registry;

import lombok.Data;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Data
@Getter
public class YamlRegistryMapping {
    public String id;
    public String host;
}
