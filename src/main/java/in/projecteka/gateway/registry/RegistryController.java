package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.model.ClientResponse;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import in.projecteka.gateway.registry.model.ServiceProfileResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;

import static in.projecteka.gateway.common.Constants.APP_PATH_HI_SERVICES;
import static in.projecteka.gateway.common.Constants.HI_SERVICES_SERVICE_ID;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES_BRIDGE_ID_SERVICES;
import static in.projecteka.gateway.common.Constants.INTERNAL_CM;


@AllArgsConstructor
@RestController
public class RegistryController {
    private static final String UNSPECIFIED_SERVICE_TYPE = "UNSPECIFIED";
    private final RegistryService registryService;

    @PutMapping(INTERNAL_CM)
    public Mono<ClientResponse> cmServiceEntries(@RequestBody CMServiceRequest cmServiceRequest) {
        return registryService.populateCMEntry(cmServiceRequest);
    }

    @PutMapping(INTERNAL_BRIDGES)
    public Mono<ClientResponse> bridgeEntry(@Valid @RequestBody BridgeRegistryRequest bridgeRegistryRequest) {
        return registryService.populateBridgeEntry(bridgeRegistryRequest);
    }

    @PutMapping(INTERNAL_BRIDGES_BRIDGE_ID_SERVICES)
    public Mono<Void> bridgeServiceEntries(@PathVariable("bridgeId") String bridgeId,
                                           @RequestBody List<BridgeServiceRequest> bridgeServicesRequest) {
        return registryService.populateBridgeServicesEntries(bridgeId, bridgeServicesRequest);
    }

    @GetMapping(HI_SERVICES_SERVICE_ID)
    public Mono<ServiceProfileResponse> serviceProfile(@PathVariable("service-id") String serviceId) {
        return registryService.serviceProfile(serviceId);
    }

    @GetMapping(APP_PATH_HI_SERVICES)
    public Mono<List<ServiceProfileResponse>> serviceProfilesForType(@RequestParam(defaultValue = UNSPECIFIED_SERVICE_TYPE) String type) {
        return registryService.servicesOfType(type);
    }
}
