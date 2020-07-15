package in.projecteka.gateway.common;

import in.projecteka.gateway.common.model.Service;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class MappingService {

    private MappingRepository mappingRepository;

    public Mono<Service> fetchDependentServiceUrls() {
        return mappingRepository.selectBridgeUrls().collectList()
                .flatMap(bridgeProperties -> mappingRepository.selectConsentManagerUrls().collectList()
                        .flatMap(consentManagerProperties ->
                            Mono.just(Service.builder()
                                    .bridgeProperties(bridgeProperties)
                                    .consentManagerProperties(consentManagerProperties)
                                    .build())));
    }
}
