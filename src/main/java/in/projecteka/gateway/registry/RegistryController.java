package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.model.ClientResponse;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import in.projecteka.gateway.registry.model.FacilityRepresentation;
import in.projecteka.gateway.registry.model.HFRBridgeResponse;
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

import static in.projecteka.gateway.common.Constants.GW_PATH_HI_SERVICES;
import static in.projecteka.gateway.common.Constants.GW_PATH_HI_SERVICE_BY_ID;
import static in.projecteka.gateway.common.Constants.HFR_BRIDGES_BRIDGE_ID;
import static in.projecteka.gateway.common.Constants.HFR_BRIDGES_BRIDGE_ID_SERVICES;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES_BRIDGE_ID_SERVICES;
import static in.projecteka.gateway.common.Constants.INTERNAL_CM;
import static in.projecteka.gateway.common.Constants.INTERNAL_GET_FACILITY_BY_ID;
import static in.projecteka.gateway.common.Constants.INTERNAL_SEARCH_FACILITY_BY_NAME;
import static in.projecteka.gateway.common.Constants.UNSPECIFIED_SERVICE_TYPE;


@AllArgsConstructor
@RestController
public class RegistryController {
    private final RegistryService registryService;

    @PutMapping(INTERNAL_CM)
    public Mono<ClientResponse> cmServiceEntries(@RequestBody CMServiceRequest cmServiceRequest) {
        return registryService.populateCMEntry(cmServiceRequest);
    }

    @PutMapping(INTERNAL_BRIDGES)
    public Mono<ClientResponse> bridgeEntry(@Valid @RequestBody BridgeRegistryRequest bridgeRegistryRequest) {
        return registryService.populateBridgeEntry(bridgeRegistryRequest);
    }

    @PutMapping(value = {INTERNAL_BRIDGES_BRIDGE_ID_SERVICES, HFR_BRIDGES_BRIDGE_ID_SERVICES})
    public Mono<Void> bridgeServiceEntries(@PathVariable("bridgeId") String bridgeId,
                                           @Valid @RequestBody List<BridgeServiceRequest> bridgeServicesRequest) {
        return registryService.populateBridgeServicesEntries(bridgeId, bridgeServicesRequest);
    }

    @GetMapping(GW_PATH_HI_SERVICE_BY_ID)
    public Mono<ServiceProfileResponse> serviceProfile(@PathVariable("service-id") String serviceId) {
        return registryService.serviceProfile(serviceId);
    }

    @GetMapping(GW_PATH_HI_SERVICES)
    public Mono<List<ServiceProfileResponse>> serviceProfilesForType(@RequestParam(defaultValue = UNSPECIFIED_SERVICE_TYPE) String type) {
        return registryService.servicesOfType(type);
    }

    @GetMapping(HFR_BRIDGES_BRIDGE_ID)
    public Mono<HFRBridgeResponse> bridgeProfile(@PathVariable("bridgeId") String bridgeId) {
        return registryService.bridgeProfile(bridgeId);
    }

    @GetMapping(INTERNAL_SEARCH_FACILITY_BY_NAME)
    public Mono<List<FacilityRepresentation>> searchFacilityByName(@RequestParam String name,
                                                                   @RequestParam(required = false) String state,
                                                                   @RequestParam(required = false) String district) {
        return registryService.searchFacilityByName(name, state, district);
    }

    @GetMapping(INTERNAL_GET_FACILITY_BY_ID)
    public Mono<FacilityRepresentation> searchFacilityByName(@PathVariable String serviceId) {
        return registryService.getFacilityById(serviceId);
    }
}
