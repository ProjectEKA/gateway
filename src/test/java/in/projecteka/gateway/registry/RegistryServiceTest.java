package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.CMEntry;
import in.projecteka.gateway.registry.model.KeycloakClientCredentials;
import in.projecteka.gateway.registry.model.KeycloakClientSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.test.StepVerifier;

import static in.projecteka.gateway.registry.TestBuilders.cmServiceRequest;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;
import static in.projecteka.gateway.registry.ServiceType.HIP;
import static in.projecteka.gateway.registry.TestBuilders.bridgeRegistryRequest;
import static in.projecteka.gateway.registry.TestBuilders.bridgeServiceRequest;
import static in.projecteka.gateway.registry.TestBuilders.realmRole;
import static in.projecteka.gateway.registry.TestBuilders.serviceAccount;
import static in.projecteka.gateway.registry.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
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
        registryService = Mockito.spy(new RegistryService(registryRepository, consentManagerMappings, bridgeMappings, adminServiceClient));
    }

    @Test
    void shouldInsertCMEntryIfItDoesNotExistAndCreateClient() {
        var request = cmServiceRequest().isActive(true).build();
        var cmEntry = CMEntry.builder().isExists(false).build();
        var clientSecret = KeycloakClientSecret.builder().build();
        var keyCloakCreds = KeycloakClientCredentials.builder()
                .key(request.getSuffix())
                .secret(clientSecret.getValue())
                .build();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("CM").build());

        when(registryRepository.getActiveStatusIfPresent(request.getSuffix())).thenReturn(Mono.just(cmEntry));
        when(registryRepository.createCMEntry(request)).thenReturn(Mono.empty());
        when(adminServiceClient.createClient(request.getSuffix())).thenReturn(Mono.empty());
        when(adminServiceClient.getServiceAccount(request.getSuffix())).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());
        when(adminServiceClient.getClientSecret(request.getSuffix())).thenReturn(Mono.just(clientSecret));


        StepVerifier.create(registryService.populateCMEntry(request)).expectNext(keyCloakCreds).verifyComplete();

        verify(registryRepository).getActiveStatusIfPresent(request.getSuffix());
        verify(registryRepository).createCMEntry(request);
        verify(adminServiceClient).createClient(request.getSuffix());
        verify(adminServiceClient).getServiceAccount(request.getSuffix());
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
        verify(adminServiceClient).getClientSecret(request.getSuffix());
    }

    @Test
    void shouldUpdateCMEntryIfItExists() {
        var request = cmServiceRequest().isActive(true).build();
        var cmEntry = CMEntry.builder().isExists(true).isActive(true).build();

        when(registryRepository.getActiveStatusIfPresent(request.getSuffix())).thenReturn(Mono.just(cmEntry));
        when(registryRepository.updateCMEntry(request)).thenReturn(Mono.create(MonoSink::success));
        when(consentManagerMappings.invalidate(request.getSuffix())).thenReturn(Mono.empty());

        StepVerifier.create(registryService.populateCMEntry(request)).verifyComplete();

        verify(registryRepository, times(1)).getActiveStatusIfPresent(request.getSuffix());
        verify(registryRepository, times(1)).updateCMEntry(request);
        verify(consentManagerMappings).invalidate(request.getSuffix());
    }

    @Test
    void shouldUpdateCMEntryIfItExistsAndCreateClientIfItIsNotActiveBefore() {
        var request = cmServiceRequest().isActive(true).build();
        var cmEntry = CMEntry.builder().isExists(true).isActive(false).build();
        var clientSecret = KeycloakClientSecret.builder().build();
        var keyCloakCreds = KeycloakClientCredentials.builder()
                .key(request.getSuffix())
                .secret(clientSecret.getValue())
                .build();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("CM").build());

        when(registryRepository.getActiveStatusIfPresent(request.getSuffix())).thenReturn(Mono.just(cmEntry));
        when(registryRepository.updateCMEntry(request)).thenReturn(Mono.create(MonoSink::success));
        when(consentManagerMappings.invalidate(request.getSuffix())).thenReturn(Mono.empty());
        when(adminServiceClient.createClient(request.getSuffix())).thenReturn(Mono.empty());
        when(adminServiceClient.getServiceAccount(request.getSuffix())).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());
        when(adminServiceClient.getClientSecret(request.getSuffix())).thenReturn(Mono.just(clientSecret));

        StepVerifier.create(registryService.populateCMEntry(request)).expectNext(keyCloakCreds).verifyComplete();

        verify(registryRepository, times(1)).getActiveStatusIfPresent(request.getSuffix());
        verify(registryRepository, times(1)).updateCMEntry(request);
        verify(consentManagerMappings).invalidate(request.getSuffix());
        verify(adminServiceClient).createClient(request.getSuffix());
        verify(adminServiceClient).getServiceAccount(request.getSuffix());
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
        verify(adminServiceClient).getClientSecret(request.getSuffix());
    }

    @Test
    void shouldCreateBridgeEntryAndClientInKeyCloak() {
        var request = bridgeRegistryRequest().active(true).build();
        var bridgeId = request.getId();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(false));
        when(registryRepository.insertBridgeEntry(request)).thenReturn(empty());
        when(adminServiceClient.createClient(bridgeId)).thenReturn(empty());

        var producer = registryService.populateBridgeEntry(request);
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(bridgeId);
        verify(registryRepository).insertBridgeEntry(request);
        verify(adminServiceClient).createClient(bridgeId);
    }

    @Test
    void shouldUpdateBridgeEntryWhenBridgeIsActive() {
        var request = bridgeRegistryRequest().active(true).build();
        var bridgeId = request.getId();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(true));
        when(registryRepository.updateBridgeEntry(request)).thenReturn(empty());
        when(adminServiceClient.createClient(bridgeId)).thenReturn(empty());

        var producer = registryService.populateBridgeEntry(request);
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(bridgeId);
        verify(registryRepository).updateBridgeEntry(request);
        verify(adminServiceClient).createClient(bridgeId);
    }

    @Test
    void shouldUpdateBridgeEntryAndDeleteClientInKeyCloakWhenBridgeIsInactive() {
        var request = bridgeRegistryRequest().active(false).build();
        var bridgeId = request.getId();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(true));
        when(registryRepository.updateBridgeEntry(request)).thenReturn(empty());
        when(adminServiceClient.deleteClient(bridgeId)).thenReturn(empty());

        var producer = registryService.populateBridgeEntry(request);
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(bridgeId);
        verify(registryRepository).updateBridgeEntry(request);
        verify(adminServiceClient).deleteClient(bridgeId);
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
        when(registryRepository.ifPresent(request.getId(), request.getType(), request.isActive()))
                .thenReturn(just(true));

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyErrorSatisfies(throwable ->
                        assertThat(throwable).isEqualToComparingFieldByField(invalidBridgeServiceRequest()));

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive());

    }

    @Test
    void shouldCreateBridgeServiceEntriesAndAddClientRoles() {
        var request = bridgeServiceRequest().active(true).type(HIP).build();
        var bridgeId = string();
        var serviceAccount = serviceAccount().build();
        var realmRoles = List.of(realmRole().name("HIP").build(), realmRole().name("HIU").build());
        when(registryRepository.ifPresent(request.getId(), request.getType(), request.isActive()))
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

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive());
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
        when(registryRepository.ifPresent(request.getId(), request.getType(), request.isActive()))
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

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive());
        verify(registryRepository).ifPresent(request.getId(), request.getType());
        verify(registryRepository).updateBridgeServiceEntry(bridgeId, request);
        verify(bridgeMappings).invalidate(Pair.of(request.getId(), request.getType()));
        verify(adminServiceClient).getServiceAccount(bridgeId);
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
    }
}
