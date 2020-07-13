package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.Model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.Model.BridgeServiceRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
public class RegistryController {
    private final RegistryService registryService;

    @PostMapping("/internal/bridge")
    public Mono<Void> bridgeEntry(@RequestBody BridgeRegistryRequest bridgeRegistryRequest) {
        return registryService.populateBridgeEntry(bridgeRegistryRequest);
    }

    @PostMapping("/bridges/101/services")
    public Mono<Void> bridgeServiceEntries(@RequestBody BridgeServiceRequest bridgeServiceRequest) {
        return registryService.populateBridgeServiceEntries(bridgeServiceRequest);
    }
}
