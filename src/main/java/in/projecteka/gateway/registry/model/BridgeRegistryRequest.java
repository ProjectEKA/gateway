package in.projecteka.gateway.registry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeRegistryRequest {
    @NotBlank(message = "id is not specified")
    String id;
    String name;
    String url;
    Boolean active;
    Boolean blocklisted;
}
