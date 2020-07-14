package in.projecteka.gateway.common;

import in.projecteka.gateway.common.model.Path;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class MappingService {

    private MappingRepository mappingRepository;

    public Mono<Path> getAllUrls() {
        return mappingRepository.selectBridgeUrls().collectList()
                .flatMap(bridgeUrls -> Mono.just(Path.builder().bridgeUrls(bridgeUrls).build()));
    }
}
