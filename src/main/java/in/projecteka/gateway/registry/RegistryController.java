package in.projecteka.gateway.registry;

import in.projecteka.gateway.registry.model.CMServiceRequest;
import in.projecteka.gateway.registry.model.KeycloakClientCredentials;
import lombok.AllArgsConstructor;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES_BRIDGE_ID_SERVICES;

@AllArgsConstructor
@RestController
public class RegistryController {
    private final RegistryService registryService;

    @PutMapping("/internal/cm")
    public Mono<KeycloakClientCredentials> cmServiceEntries(@RequestBody CMServiceRequest cmServiceRequest) {
        return registryService.populateCMEntry(cmServiceRequest);
    }

    @PutMapping(INTERNAL_BRIDGES)
    public Mono<Void> bridgeEntry(@RequestBody BridgeRegistryRequest bridgeRegistryRequest) {
        return registryService.populateBridgeEntry(bridgeRegistryRequest);
    }

    @PutMapping(INTERNAL_BRIDGES_BRIDGE_ID_SERVICES)
    public Mono<Void> bridgeServiceEntries(@PathVariable("bridgeId") String bridgeId,
                                           @RequestBody List<BridgeServiceRequest> bridgeServicesRequest) {
        return registryService.populateBridgeServicesEntries(bridgeId, bridgeServicesRequest);
    }
}
