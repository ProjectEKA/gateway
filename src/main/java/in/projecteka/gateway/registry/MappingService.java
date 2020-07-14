package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
public class MappingService {

    private MappingRepository mappingRepository;

    public Mono<Bridge> getUrl() {
        List<String> urlsList = mappingRepository.selectbridgeUrls().collectList().block();
        Bridge bridge = Bridge.builder().bridgeUrls(urlsList).build();
        return Mono.just(bridge);
    }
}
