package in.projecteka.gateway.registry.model;

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
