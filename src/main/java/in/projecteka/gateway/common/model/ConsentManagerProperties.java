package in.projecteka.gateway.common.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ConsentManagerProperties {
    private String name;
    private String url;
    private String id;
}
