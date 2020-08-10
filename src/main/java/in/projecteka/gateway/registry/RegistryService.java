package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.model.ClientResponse;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.Bridge;
import in.projecteka.gateway.registry.model.BridgeRegistryRequest;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import in.projecteka.gateway.registry.model.CMEntry;
import in.projecteka.gateway.registry.model.CMServiceRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeRegistryRequest;
import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;
import static in.projecteka.gateway.clients.ClientError.invalidCMEntry;
import static in.projecteka.gateway.clients.ClientError.invalidCMRegistryRequest;

@AllArgsConstructor
public class RegistryService {
    private static final String CM_REALM_ROLE = "CM";
    private final RegistryRepository registryRepository;
    private final CacheAdapter<String, String> consentManagerMappings;
    private final CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;
    private final AdminServiceClient adminServiceClient;

    public Mono<ClientResponse> populateCMEntry(CMServiceRequest request) {
        return Mono.just(request)
                .filterWhen(this::validateRequest)
                .flatMap(req -> updateCMRequest(request))
                .flatMap(updatedRequest -> registryRepository.getCMEntryIfActive(updatedRequest.getSuffix())
                        .flatMap(cmEntry -> cmEntry.isExists()
                                ? updateCMEntry(cmEntry, updatedRequest)
                                : createCMEntry(updatedRequest)
                        ));
    }

    private Mono<CMServiceRequest> updateCMRequest(CMServiceRequest request) {
        var updateCMServiceRequest = request.toBuilder();

        if (request.getIsActive() == null)
            updateCMServiceRequest.isActive(true);
        if (request.getIsBlocklisted() == null)
            updateCMServiceRequest.isBlocklisted(false);

        return Mono.just(updateCMServiceRequest.build());
    }

    private Mono<Boolean> validateRequest(CMServiceRequest request) {
        if (request != null
                && ((request.getSuffix() == null || request.getSuffix().isBlank())
                || (request.getUrl() == null || request.getUrl().isBlank())) )
            return Mono.error(invalidCMRegistryRequest());

        return Mono.just(true);
    }

    private Mono<ClientResponse> updateCMEntry(CMEntry cmEntry, CMServiceRequest request) {
        return registryRepository.updateCMEntry(request)
                .then(consentManagerMappings.invalidate(request.getSuffix()))
                .then(updateClients(cmEntry, request));
    }

    private Mono<ClientResponse> createCMEntry(CMServiceRequest request) {
        if (Boolean.TRUE.equals(request.getIsActive()))
            return registryRepository.createCMEntry(request)
                    .then(createClientAndAddRole(request.getSuffix()));
        return Mono.error(invalidCMEntry());
    }

    private Mono<ClientResponse> updateClients(CMEntry oldCMEntry, CMServiceRequest newCMEntry) {
        if (Boolean.compare(oldCMEntry.isActive(), newCMEntry.getIsActive()) == 0) {
            return Mono.empty();
        }
        if (Boolean.TRUE.equals(newCMEntry.getIsActive())) {
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

    public Mono<ClientResponse> populateBridgeEntry(BridgeRegistryRequest bridgeRegistryRequest) {
        return registryRepository.ifPresent(bridgeRegistryRequest.getId())
                .flatMap(bridge -> bridge.getId() != null
                        ? bridgeRequest(bridge, bridgeRegistryRequest)
                        .flatMap(req -> registryRepository.updateBridgeEntry(req)
                                .then(req.getActive()
                                        ? createClient(bridgeRegistryRequest.getId())
                                        : adminServiceClient.deleteClientIfExists(bridgeRegistryRequest.getId())
                                        .then(Mono.empty())
                                ))
                        : bridgeRegistryRequest.getActive() == null
                        ? Mono.error(invalidBridgeRegistryRequest("Invalid request"))
                        : bridgeRegistryRequest.getActive()
                        ? registryRepository.insertBridgeEntry(bridgeRegistryRequest)
                        .then(createClient(bridgeRegistryRequest.getId()))
                        : Mono.error(invalidBridgeRegistryRequest("can't register an inactive bridge")));
    }

    private Mono<BridgeRegistryRequest> bridgeRequest(Bridge bridge, BridgeRegistryRequest request) {
        var bridgeRequest = BridgeRegistryRequest.builder()
                .id(request.getId())
                .name(request.getName() == null ? bridge.getName() : request.getName())
                .url(request.getUrl() == null ? bridge.getUrl() : request.getUrl())
                .active(request.getActive() == null ? bridge.getActive() : request.getActive())
                .blocklisted(request.getBlocklisted() == null ? bridge.getBlocklisted() : request.getBlocklisted())
                .build();
        return Mono.just(bridgeRequest);
    }

    private Mono<ClientResponse> createClient(String bridgeId) {
        return adminServiceClient.createClientIfNotExists(bridgeId)
                .then(adminServiceClient.getClientSecret(bridgeId)
                        .map(clientSecret -> ClientResponse.builder()
                                .id(bridgeId)
                                .secret(clientSecret.getValue())
                                .build()));
    }

    public Mono<Void> populateBridgeServicesEntries(String bridgeId, List<BridgeServiceRequest> bridgeServicesRequest) {
        return Flux.fromIterable(bridgeServicesRequest)
                .flatMap(request -> request.isActive()
                        ? registryRepository.ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId)
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
