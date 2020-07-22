package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.CMEntry;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import lombok.AllArgsConstructor;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.model.ClientResponse;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeRegistryRequest;
import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;
import static in.projecteka.gateway.clients.ClientError.invalidCMRegistryRequest;
import static in.projecteka.gateway.registry.EntryStatus.NOT_EXISTS;
import static reactor.core.publisher.Mono.empty;

@AllArgsConstructor
public class RegistryService {
    private static final String CM_REALM_ROLE = "CM";
    private final RegistryRepository registryRepository;
    private final CacheAdapter<String, String> consentManagerMappings;
    private final CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;
    private final AdminServiceClient adminServiceClient;

    public Mono<ClientResponse> populateCMEntry(CMServiceRequest request) {
        return registryRepository.getActiveStatusIfPresent(request.getSuffix())
                .flatMap(cmEntry -> cmEntry.isExists()
                        ? updateCMEntry(cmEntry, request)
                        : createCMEntry(request)
                );
    }

    private Mono<ClientResponse> updateCMEntry(CMEntry cmEntry, CMServiceRequest request) {
        return registryRepository.updateCMEntry(request)
                .then(consentManagerMappings.invalidate(request.getSuffix()))
                .then(updateClients(cmEntry, request));
    }

    private Mono<ClientResponse> createCMEntry(CMServiceRequest request) {
        if (request.getIsActive())
            return registryRepository.createCMEntry(request)
                    .then(createClientAndAddRole(request.getSuffix()));
        return Mono.error(invalidCMRegistryRequest());
    }

    private Mono<ClientResponse> updateClients(CMEntry oldCMEntry, CMServiceRequest newCMEntry) {
        if (oldCMEntry.isActive() == newCMEntry.getIsActive()) {
            return Mono.empty();
        }
        if (newCMEntry.getIsActive()) {
            return createClientAndAddRole(newCMEntry.getSuffix());
        }
        return adminServiceClient.deleteClient(newCMEntry.getSuffix()).then(Mono.empty());
    }

    private Mono<ClientResponse> createClientAndAddRole(String suffix) {
        return adminServiceClient.createClient(suffix)
                .then(addRole(suffix, CM_REALM_ROLE))
                .then(adminServiceClient.getClientSecret(suffix))
                .map(keycloakClientSecret ->
                        ClientResponse.builder()
                                .id(suffix)
                                .secret(keycloakClientSecret.getValue())
                                .build());
    }


    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public Mono<ClientResponse> populateBridgeEntry(BridgeRegistryRequest bridgeRegistryRequest) {
        return registryRepository.ifPresent(bridgeRegistryRequest.getId())
                .flatMap(res -> !res.equals(NOT_EXISTS.name())
                        ? registryRepository.updateBridgeEntry(bridgeRegistryRequest)
                        .thenReturn(res)
                        .flatMap(result -> Boolean.parseBoolean(result) != bridgeRegistryRequest.isActive()
                                ? bridgeRegistryRequest.isActive()
                                ? createClient(bridgeRegistryRequest.getId())
                                : adminServiceClient.deleteClient(bridgeRegistryRequest.getId()).then(empty())
                                : empty())
                        : bridgeRegistryRequest.isActive()
                        ? registryRepository.insertBridgeEntry(bridgeRegistryRequest)
                        .then(createClient(bridgeRegistryRequest.getId()))
                        : Mono.error(invalidBridgeRegistryRequest()));
    }

    private Mono<ClientResponse> createClient(String bridgeId) {
        return adminServiceClient.createClient(bridgeId)
                .then(adminServiceClient.getClientSecret(bridgeId)
                        .map(clientSecret -> ClientResponse.builder()
                                .id(bridgeId)
                                .secret(clientSecret.getValue())
                                .build()));
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

    private Mono<Void> addRole(String clientId, String type) {
        return adminServiceClient.getServiceAccount(clientId)
                .flatMap(serviceAccount -> adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())
                        .flatMap(realmRoles -> ifPresent(type, realmRoles)
                                .flatMap(realmRole -> adminServiceClient.assignRoleToClient(List.of(realmRole), serviceAccount.getId()))));
    }

    private Mono<RealmRole> ifPresent(String type, List<RealmRole> realmRoles) {
        return Mono.justOrEmpty(realmRoles.stream().filter(realmRole -> realmRole.getName().equalsIgnoreCase(type)).findFirst());
    }
}
