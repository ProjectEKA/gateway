package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.Model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.Model.BridgeServiceRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;

@AllArgsConstructor
public class RegistryService {
    private final RegistryRepository registryRepository;
    private final CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;
    private final AdminServiceClient adminServiceClient;

    public Mono<Void> populateBridgeEntry(BridgeRegistryRequest bridgeRegistryRequest) {
        return registryRepository.ifPresent(bridgeRegistryRequest.getId())
                .flatMap(result -> result
                        ? registryRepository.updateBridgeEntry(bridgeRegistryRequest)
                        : registryRepository.insertBridgeEntry(bridgeRegistryRequest))
                .then(adminServiceClient.createClient(bridgeRegistryRequest.getId()));
    }

    public Mono<Void> populateBridgeServicesEntries(String bridgeId, List<BridgeServiceRequest> bridgeServicesRequest) {
        return Flux.fromIterable(bridgeServicesRequest)
                .flatMap(request -> request.isActive()
                        ? registryRepository.ifPresent(request.getId(), request.getType(), request.isActive())
                        .flatMap(result -> result
                                ? Mono.error(invalidBridgeServiceRequest("Duplicate entry/Can't be serviced by multiple bridges"))
                                : populateBridgeServiceEntry(bridgeId, request))
                        : populateBridgeServiceEntry(bridgeId, request)).then();
    }

    private Mono<Void> populateBridgeServiceEntry(String bridgeId, BridgeServiceRequest request) {
        return registryRepository.ifPresent(request.getId(), request.getType())
                .flatMap(result -> result
                        ? registryRepository.updateBridgeServiceEntry(bridgeId, request)
                        : registryRepository.insertBridgeServiceEntry(bridgeId, request))
                .then(bridgeMappings.invalidate(Pair.of(request.getId(), request.getType())))
                .then(adminServiceClient.getServiceAccount(bridgeId)
                        .flatMap(serviceAccount -> adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())
                                .flatMap(realmRoles -> ifPresent(request.getType().toString(), realmRoles)
                                        .flatMap(realmRole -> adminServiceClient.assignRoleToClient(List.of(realmRole), serviceAccount.getId())))));
    }

    private Mono<RealmRole> ifPresent(String type, List<RealmRole> realmRoles) {
        return Mono.justOrEmpty(realmRoles.stream().filter(realmRole -> realmRole.getName().equalsIgnoreCase(type)).findFirst());
    }
}
