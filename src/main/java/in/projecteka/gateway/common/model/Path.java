package in.projecteka.gateway.common.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class Path {
    private List<String> bridgeUrls;
}
