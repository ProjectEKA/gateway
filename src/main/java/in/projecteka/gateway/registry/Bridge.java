package in.projecteka.gateway.registry;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class Bridge {
    private List<String> bridgeUrls;
}
