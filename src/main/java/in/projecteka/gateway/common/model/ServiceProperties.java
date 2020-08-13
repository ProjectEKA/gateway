package in.projecteka.gateway.common.model;

import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;

@Builder
@Value
public class ServiceProperties {
    private String name;
    private String url;
    private String id;
    @Nullable
    private String type;
}
