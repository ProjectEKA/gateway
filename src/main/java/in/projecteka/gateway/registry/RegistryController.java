package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.Model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.Model.BridgeServiceRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
@RestController
public class RegistryController {
    private final RegistryService registryService;

    @PostMapping("/internal/bridges")
    public Mono<Void> bridgeEntry(@RequestBody BridgeRegistryRequest bridgeRegistryRequest) {
        return registryService.populateBridgeEntry(bridgeRegistryRequest);
    }

    @PostMapping("/internal/bridges/{bridgeId}/services")
    public Mono<Void> bridgeServiceEntries(@PathVariable("bridgeId") String bridgeId,
                                           @RequestBody List<BridgeServiceRequest> bridgeServicesRequest) {
        return registryService.populateBridgeServicesEntries(bridgeId, bridgeServicesRequest);
    }
}
