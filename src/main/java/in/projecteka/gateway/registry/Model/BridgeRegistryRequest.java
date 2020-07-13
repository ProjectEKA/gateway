package in.projecteka.gateway.registry.Model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BridgeRegistryRequest {
    private String id;
    private String name;
    private String url;
    private boolean active;
    private boolean blocklisted;
}
