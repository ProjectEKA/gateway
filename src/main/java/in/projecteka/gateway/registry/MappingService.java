package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

@AllArgsConstructor
public class MappingService {

    private MappingRepository mappingRepository;

    public Flux<String> getUrls() {
        return mappingRepository.selectBridgeUrls();
    }
}
