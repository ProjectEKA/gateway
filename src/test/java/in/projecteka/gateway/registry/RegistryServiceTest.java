package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.model.ClientResponse;
import in.projecteka.gateway.clients.model.ClientSecret;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.Bridge;
import in.projecteka.gateway.registry.model.CMEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.test.StepVerifier;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeRegistryRequest;
import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;
import static in.projecteka.gateway.clients.ClientError.invalidCMEntry;
import static in.projecteka.gateway.clients.ClientError.invalidCMRegistryRequest;
import static in.projecteka.gateway.registry.ServiceType.HIP;
import static in.projecteka.gateway.registry.TestBuilders.bridge;
import static in.projecteka.gateway.registry.TestBuilders.bridgeRegistryRequest;
import static in.projecteka.gateway.registry.TestBuilders.bridgeServiceRequest;
import static in.projecteka.gateway.registry.TestBuilders.cmServiceRequest;
import static in.projecteka.gateway.registry.TestBuilders.realmRole;
import static in.projecteka.gateway.registry.TestBuilders.serviceAccount;
import static in.projecteka.gateway.registry.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestBuilders.clientSecret;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

class RegistryServiceTest {

    @Mock
    @Qualifier("consentManagerMappings")
    CacheAdapter<String, String> consentManagerMappings;

    @Mock
    CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;

    @Mock
    RegistryRepository registryRepository;

    @Mock
    AdminServiceClient adminServiceClient;

    @Mock
    RegistryService registryService;

    @BeforeEach
    void init() {
        initMocks(this);
        registryService = Mockito.spy(new RegistryService(
                false, registryRepository, consentManagerMappings, bridgeMappings, adminServiceClient
        ));
    }

    @Test
    void shouldInsertCMEntryIfItDoesNotExistAndCreateClient() {
        var request = cmServiceRequest().isActive(true).build();
        var cmEntry = CMEntry.builder().isExists(false).build();
        var clientSecret = ClientSecret.builder().build();
        var keyCloakCreds = ClientResponse.builder()
                .id(request.getSuffix())
                .secret(clientSecret.getValue())
                .build();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("CM").build());

        when(registryRepository.getCMEntryIfActive(request.getSuffix())).thenReturn(Mono.just(cmEntry));
        when(registryRepository.createCMEntry(request)).thenReturn(Mono.empty());
        when(adminServiceClient.createClient(request.getSuffix())).thenReturn(Mono.empty());
        when(adminServiceClient.getServiceAccount(request.getSuffix())).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());
        when(adminServiceClient.getClientSecret(request.getSuffix())).thenReturn(Mono.just(clientSecret));


        StepVerifier.create(registryService.populateCMEntry(request)).expectNext(keyCloakCreds).verifyComplete();

        verify(registryRepository).getCMEntryIfActive(request.getSuffix());
        verify(registryRepository).createCMEntry(request);
        verify(adminServiceClient).createClient(request.getSuffix());
        verify(adminServiceClient).getServiceAccount(request.getSuffix());
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
        verify(adminServiceClient).getClientSecret(request.getSuffix());
    }

    @Test
    void shouldThrowErrorAndNotCreateCMEntryWhenActiveIsFalse() {
        var request = cmServiceRequest().isActive(false).build();
        var cmEntry = CMEntry.builder().isExists(false).build();

        when(registryRepository.getCMEntryIfActive(request.getSuffix())).thenReturn(Mono.just(cmEntry));

        StepVerifier.create(registryService.populateCMEntry(request))
                .verifyErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(invalidCMEntry()));

        verify(registryRepository).getCMEntryIfActive(request.getSuffix());
    }

    @Test
    void shouldThrowErrorForIfURLIsEmpty() {
        var request = cmServiceRequest().url("").build();

        StepVerifier.create(registryService.populateCMEntry(request))
                .verifyErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(invalidCMRegistryRequest()));
    }

    @Test
    void shouldThrowErrorForIfCMSuffixIsEmpty() {
        var request = cmServiceRequest().suffix("").build();

        StepVerifier.create(registryService.populateCMEntry(request))
                .verifyErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(invalidCMRegistryRequest()));
    }

    @Test
    void shouldUpdateCMEntryIfItExists() {
        var request = cmServiceRequest().isActive(true).build();
        var cmEntry = CMEntry.builder().isExists(true).isActive(true).build();

        when(registryRepository.getCMEntryIfActive(request.getSuffix())).thenReturn(Mono.just(cmEntry));
        when(registryRepository.updateCMEntry(request)).thenReturn(Mono.create(MonoSink::success));
        when(consentManagerMappings.invalidate(request.getSuffix())).thenReturn(Mono.empty());

        StepVerifier.create(registryService.populateCMEntry(request)).verifyComplete();

        verify(registryRepository, times(1)).getCMEntryIfActive(request.getSuffix());
        verify(registryRepository, times(1)).updateCMEntry(request);
        verify(consentManagerMappings).invalidate(request.getSuffix());
    }

    @Test
    void shouldUpdateCMEntryIfItExistsAndCreateClientIfItIsNotActiveBefore() {
        var request = cmServiceRequest().isActive(true).build();
        var cmEntry = CMEntry.builder().isExists(true).isActive(false).build();
        var clientSecret = ClientSecret.builder().build();
        var keyCloakCreds = ClientResponse.builder()
                .id(request.getSuffix())
                .secret(clientSecret.getValue())
                .build();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("CM").build());

        when(registryRepository.getCMEntryIfActive(request.getSuffix())).thenReturn(Mono.just(cmEntry));
        when(registryRepository.updateCMEntry(request)).thenReturn(Mono.create(MonoSink::success));
        when(consentManagerMappings.invalidate(request.getSuffix())).thenReturn(Mono.empty());
        when(adminServiceClient.createClient(request.getSuffix())).thenReturn(Mono.empty());
        when(adminServiceClient.getServiceAccount(request.getSuffix())).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());
        when(adminServiceClient.getClientSecret(request.getSuffix())).thenReturn(Mono.just(clientSecret));

        StepVerifier.create(registryService.populateCMEntry(request)).expectNext(keyCloakCreds).verifyComplete();

        verify(registryRepository, times(1)).getCMEntryIfActive(request.getSuffix());
        verify(registryRepository, times(1)).updateCMEntry(request);
        verify(consentManagerMappings).invalidate(request.getSuffix());
        verify(adminServiceClient).createClient(request.getSuffix());
        verify(adminServiceClient).getServiceAccount(request.getSuffix());
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
        verify(adminServiceClient).getClientSecret(request.getSuffix());
    }

    @Test
    void shouldDeleteClientIfItExistsAndIsNotActive() {
        var request = cmServiceRequest().isActive(false).build();
        var cmEntry = CMEntry.builder().isExists(true).isActive(true).build();

        when(registryRepository.getCMEntryIfActive(request.getSuffix())).thenReturn(Mono.just(cmEntry));
        when(registryRepository.updateCMEntry(request)).thenReturn(Mono.create(MonoSink::success));
        when(consentManagerMappings.invalidate(request.getSuffix())).thenReturn(Mono.empty());
        when(adminServiceClient.deleteClient(request.getSuffix())).thenReturn(Mono.empty());

        StepVerifier.create(registryService.populateCMEntry(request)).verifyComplete();

        verify(registryRepository, times(1)).getCMEntryIfActive(request.getSuffix());
        verify(registryRepository, times(1)).updateCMEntry(request);
        verify(consentManagerMappings).invalidate(request.getSuffix());
        verify(adminServiceClient).deleteClient(request.getSuffix());
    }

    @Test
    void shouldCreateBridgeEntryAndClientInKeyCloak() {
        var request = bridgeRegistryRequest().active(true).build();
        var bridgeId = request.getId();
        var clientSecret = clientSecret().build();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("healthId").build());
        var clientResponse = ClientResponse.builder().id(bridgeId).secret(clientSecret.getValue()).build();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(Bridge.builder().build()));
        when(registryRepository.insertBridgeEntry(request)).thenReturn(empty());
        when(adminServiceClient.createClientIfNotExists(bridgeId)).thenReturn(empty());
        when(adminServiceClient.getServiceAccount(bridgeId)).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());
        when(adminServiceClient.getClientSecret(bridgeId)).thenReturn(just(clientSecret));

        var producer = registryService.populateBridgeEntry(request);
        StepVerifier.create(producer)
                .expectNext(clientResponse)
                .verifyComplete();

        verify(registryRepository).ifPresent(bridgeId);
        verify(registryRepository).insertBridgeEntry(request);
        verify(adminServiceClient).createClientIfNotExists(bridgeId);
        verify(adminServiceClient).getClientSecret(bridgeId);
    }

    @Test
    void shouldThrowInvalidBridgeRegistryRequestErrorWhenAnInactiveBridgeRegisters() {
        var request = bridgeRegistryRequest().active(false).build();
        var bridgeId = request.getId();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(Bridge.builder().build()));

        var producer = registryService.populateBridgeEntry(request);
        StepVerifier.create(producer)
                .verifyErrorSatisfies(throwable -> assertThat(throwable)
                        .isEqualToComparingFieldByField(invalidBridgeRegistryRequest("can't register an inactive bridge")));

        verify(registryRepository).ifPresent(bridgeId);
    }

    @Test
    void shouldUpdateBridgeEntryAndDeleteClientInKeyCloakWhenBridgeSetToInactive() {
        var request = bridgeRegistryRequest().active(false).build();
        var bridgeId = request.getId();
        var bridge = bridge().build();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(bridge));
        when(registryRepository.updateBridgeEntry(request)).thenReturn(empty());
        when(adminServiceClient.deleteClientIfExists(bridgeId)).thenReturn(empty());

        var producer = registryService.populateBridgeEntry(request);
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(bridgeId);
        verify(registryRepository).updateBridgeEntry(request);
        verify(adminServiceClient).deleteClientIfExists(bridgeId);
    }

    @Test
    void shouldUpdateBridgeEntryAndCreateClientInKeyCloakWhenBridgeSetToActive() {
        var request = bridgeRegistryRequest().active(true).build();
        var bridgeId = request.getId();
        var bridge = bridge().build();
        var clientSecret = clientSecret().build();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("healthId").build());
        var clientResponse = ClientResponse.builder().id(bridgeId).secret(clientSecret.getValue()).build();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(bridge));
        when(registryRepository.updateBridgeEntry(request)).thenReturn(empty());
        when(adminServiceClient.createClientIfNotExists(bridgeId)).thenReturn(empty());
        when(adminServiceClient.getServiceAccount(bridgeId)).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());
        when(adminServiceClient.getClientSecret(bridgeId)).thenReturn(just(clientSecret));

        var producer = registryService.populateBridgeEntry(request);
        StepVerifier.create(producer)
                .expectNext(clientResponse)
                .verifyComplete();

        verify(registryRepository).ifPresent(bridgeId);
        verify(registryRepository).updateBridgeEntry(request);
        verify(adminServiceClient).createClientIfNotExists(bridgeId);
        verify(adminServiceClient).getClientSecret(bridgeId);
    }

    @Test
    void shouldCreateBridgeServiceEntries() {
        var request = bridgeServiceRequest().active(false).build();
        var bridgeId = string();
        when(registryRepository.ifPresent(request.getId(), request.getType())).thenReturn(just(false));
        when(registryRepository.insertBridgeServiceEntry(bridgeId, request)).thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(request.getId(), request.getType());
        verify(registryRepository).insertBridgeServiceEntry(bridgeId, request);
    }

    @Test
    void shouldUpdateBridgeServiceEntries() {
        var request = bridgeServiceRequest().active(false).build();
        var bridgeId = string();
        when(registryRepository.ifPresent(request.getId(), request.getType())).thenReturn(just(true));
        when(registryRepository.updateBridgeServiceEntry(bridgeId, request)).thenReturn(empty());
        when(bridgeMappings.invalidate(Pair.of(request.getId(), request.getType()))).thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(request.getId(), request.getType());
        verify(registryRepository).updateBridgeServiceEntry(bridgeId, request);
        verify(bridgeMappings).invalidate(Pair.of(request.getId(), request.getType()));
    }

    @Test
    void shouldThrowInvalidBridgeServiceRequest() {
        var request = bridgeServiceRequest().active(true).build();
        var bridgeId = string();
        when(registryRepository.ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId))
                .thenReturn(just(true));

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(invalidBridgeServiceRequest()));

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId);

    }

    @Test
    void shouldCreateBridgeServiceEntriesAndAddClientRoles() {
        var request = bridgeServiceRequest().active(true).type(HIP).build();
        var bridgeId = string();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("HIP").build(), realmRole().name("HIU").build());
        when(registryRepository.ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId))
                .thenReturn(just(false));
        when(registryRepository.ifPresent(request.getId(), request.getType())).thenReturn(just(false));
        when(registryRepository.insertBridgeServiceEntry(bridgeId, request)).thenReturn(empty());
        when(adminServiceClient.getServiceAccount(bridgeId)).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId);
        verify(registryRepository).ifPresent(request.getId(), request.getType());
        verify(registryRepository).insertBridgeServiceEntry(bridgeId, request);
        verify(adminServiceClient).getServiceAccount(bridgeId);
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
    }

    @Test
    void shouldUpdateBridgeServiceEntriesAndAddClientRolesForActiveService() {
        var request = bridgeServiceRequest().active(true).type(HIP).build();
        var bridgeId = string();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("HIP").build(), realmRole().name("HIU").build());
        when(registryRepository.ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId))
                .thenReturn(just(false));
        when(registryRepository.ifPresent(request.getId(), request.getType())).thenReturn(just(true));
        when(registryRepository.updateBridgeServiceEntry(bridgeId, request)).thenReturn(empty());
        when(bridgeMappings.invalidate(Pair.of(request.getId(), request.getType()))).thenReturn(empty());
        when(adminServiceClient.getServiceAccount(bridgeId)).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId);
        verify(registryRepository).ifPresent(request.getId(), request.getType());
        verify(registryRepository).updateBridgeServiceEntry(bridgeId, request);
        verify(bridgeMappings).invalidate(Pair.of(request.getId(), request.getType()));
        verify(adminServiceClient).getServiceAccount(bridgeId);
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
    }
}
