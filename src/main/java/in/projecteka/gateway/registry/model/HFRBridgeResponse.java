package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class HFRBridgeResponse {
    String id;
    String name;
    String url;
    Boolean active;
    Boolean blocklisted;
    LocalDateTime createdAt;
    LocalDateTime modifiedAt;
}