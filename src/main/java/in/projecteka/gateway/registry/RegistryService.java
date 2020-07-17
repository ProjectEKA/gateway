package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeRegistryRequest;
import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;

@AllArgsConstructor
public class RegistryService {
    private final RegistryRepository registryRepository;
    private final CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;
    private final AdminServiceClient adminServiceClient;

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public Mono<Void> populateBridgeEntry(BridgeRegistryRequest bridgeRegistryRequest) {
        return registryRepository.ifPresent(bridgeRegistryRequest.getId())
                .flatMap(result -> Boolean.TRUE.equals(result)
                        ? registryRepository.updateBridgeEntry(bridgeRegistryRequest)
                        .then(bridgeRegistryRequest.isActive()
                                ? adminServiceClient.createClient(bridgeRegistryRequest.getId())
                                : adminServiceClient.deleteClient(bridgeRegistryRequest.getId()))
                        : bridgeRegistryRequest.isActive()
                        ? registryRepository.insertBridgeEntry(bridgeRegistryRequest)
                        .then(adminServiceClient.createClient(bridgeRegistryRequest.getId()))
                        : Mono.error(invalidBridgeRegistryRequest()));
    }

    public Mono<Void> populateBridgeServicesEntries(String bridgeId, List<BridgeServiceRequest> bridgeServicesRequest) {
        return Flux.fromIterable(bridgeServicesRequest)
                .flatMap(request -> request.isActive()
                        ? registryRepository.ifPresent(request.getId(), request.getType(), request.isActive())
                        .flatMap(result -> Boolean.TRUE.equals(result)
                                ? Mono.error(invalidBridgeServiceRequest())
                                : populateBridgeServiceEntryAndAddRole(bridgeId, request))
                        : populateBridgeServiceEntry(bridgeId, request)).then();
    }

    private Mono<Void> populateBridgeServiceEntry(String bridgeId, BridgeServiceRequest request) {
        return upsertBridgeServiceEntry(bridgeId, request);
    }

    private Mono<Void> populateBridgeServiceEntryAndAddRole(String bridgeId, BridgeServiceRequest request) {
        return upsertBridgeServiceEntry(bridgeId, request)
                .then(addRole(bridgeId, request.getType().toString()));
    }

    private Mono<Void> upsertBridgeServiceEntry(String bridgeId, BridgeServiceRequest request) {
        return registryRepository.ifPresent(request.getId(), request.getType())
                .flatMap(result -> Boolean.TRUE.equals(result)
                        ? registryRepository.updateBridgeServiceEntry(bridgeId, request)
                        .then(bridgeMappings.invalidate(Pair.of(request.getId(), request.getType())))
                        : registryRepository.insertBridgeServiceEntry(bridgeId, request));
    }

    private Mono<Void> addRole(String bridgeId, String type) {
        return adminServiceClient.getServiceAccount(bridgeId)
                .flatMap(serviceAccount -> adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())
                        .flatMap(realmRoles -> ifPresent(type, realmRoles)
                                .flatMap(realmRole -> adminServiceClient.assignRoleToClient(List.of(realmRole), serviceAccount.getId()))));
    }

    private Mono<RealmRole> ifPresent(String type, List<RealmRole> realmRoles) {
        return Mono.justOrEmpty(realmRoles.stream().filter(realmRole -> realmRole.getName().equalsIgnoreCase(type)).findFirst());
    }
}
