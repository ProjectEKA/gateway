package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.util.Pair;
import reactor.test.StepVerifier;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;
import static in.projecteka.gateway.registry.ServiceType.HIP;
import static in.projecteka.gateway.testcommon.TestBuilders.bridgeRegistryRequest;
import static in.projecteka.gateway.testcommon.TestBuilders.bridgeServiceRequest;
import static in.projecteka.gateway.testcommon.TestBuilders.realmRole;
import static in.projecteka.gateway.testcommon.TestBuilders.serviceAccount;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

class RegistryServiceTest {

    @Mock
    RegistryRepository registryRepository;

    @Mock
    AdminServiceClient adminServiceClient;

    @Mock
    CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;

    RegistryService registryService;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        registryService = Mockito.spy(new RegistryService(registryRepository, bridgeMappings, adminServiceClient));
    }

    @Test
    void shouldCreateBridgeEntryAndClientInKeyCloak() {
        var request = bridgeRegistryRequest().build();
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
