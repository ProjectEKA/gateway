package in.projecteka.gateway.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlRegistryMapping {
    public String id;
    public String host;
    public List<String> servesAs;
}
