package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.CMEntry;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import in.projecteka.gateway.registry.model.KeycloakClientCredentials;
import lombok.AllArgsConstructor;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeRegistryRequest;
import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;
import static in.projecteka.gateway.clients.ClientError.invalidCMRegistryRequest;

@AllArgsConstructor
public class RegistryService {
    private static final String CM_REALM_ROLE = "CM";
    private final RegistryRepository registryRepository;
    private final CacheAdapter<String, String> consentManagerMappings;
    private final CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;
    private final AdminServiceClient adminServiceClient;

    public Mono<KeycloakClientCredentials> populateCMEntry(CMServiceRequest request) {
        return registryRepository.getActiveStatusIfPresent(request.getSuffix())
                .flatMap(cmEntry -> cmEntry.isExists()
                        ? updateCMEntry(cmEntry, request)
                        : createCMEntry(request)
                );
    }

    private Mono<KeycloakClientCredentials> updateCMEntry(CMEntry cmEntry, CMServiceRequest request) {
        return registryRepository.updateCMEntry(request)
                .then(consentManagerMappings.invalidate(request.getSuffix()))
                .then(updateClients(cmEntry, request));
    }

    private Mono<KeycloakClientCredentials> createCMEntry(CMServiceRequest request) {
        if (request.getIsActive())
            return registryRepository.createCMEntry(request)
                .then(createClientAndAddRole(request.getSuffix()));
        return Mono.error(invalidCMRegistryRequest());
    }

    private Mono<KeycloakClientCredentials> updateClients(CMEntry oldCMEntry, CMServiceRequest newCMEntry) {
        if (oldCMEntry.isActive() == newCMEntry.getIsActive()) {
            return Mono.empty();
        }
        if (newCMEntry.getIsActive()) {
            return createClientAndAddRole(newCMEntry.getSuffix());
        }
        return adminServiceClient.deleteClient(newCMEntry.getSuffix()).then(Mono.empty());
    }

    private Mono<KeycloakClientCredentials> createClientAndAddRole(String suffix) {
        return adminServiceClient.createClient(suffix)
                .then(addRole(suffix, CM_REALM_ROLE))
                .then(adminServiceClient.getClientSecret(suffix))
                .map(keycloakClientSecret ->
                        KeycloakClientCredentials.builder()
                                .key(suffix)
                                .secret(keycloakClientSecret.getValue())
                                .build());
    }


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
