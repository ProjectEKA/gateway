package in.projecteka.gateway.registry.model;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class CMServiceRequest {
    String name;

    @NotNull(message = "url can't be null")
    String url;

    @NotNull(message = "suffix can't be null")
    String suffix;

    @NotNull(message = "isActive can't be null")
    Boolean isActive;

    @NotNull(message = "isBlocklisted can't be null")
    Boolean isBlocklisted;
}
