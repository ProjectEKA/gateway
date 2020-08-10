package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Bridge {
    String id;
    String name;
    String url;
    Boolean active;
    Boolean blocklisted;
}