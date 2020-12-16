package in.projecteka.gateway.registry;

import in.projecteka.gateway.clients.AdminServiceClient;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.FacilityRegistryClient;
import in.projecteka.gateway.clients.model.ClientResponse;
import in.projecteka.gateway.clients.model.ClientSecret;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.model.Bridge;
import in.projecteka.gateway.registry.model.CMEntry;
import in.projecteka.gateway.registry.model.ServiceProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.test.StepVerifier;

import java.util.List;

import static in.projecteka.gateway.clients.ClientError.invalidBridgeRegistryRequest;
import static in.projecteka.gateway.clients.ClientError.invalidBridgeServiceRequest;
import static in.projecteka.gateway.clients.ClientError.invalidCMEntry;
import static in.projecteka.gateway.clients.ClientError.invalidCMRegistryRequest;
import static in.projecteka.gateway.registry.ServiceType.HIP;
import static in.projecteka.gateway.registry.ServiceType.HIU;
import static in.projecteka.gateway.registry.TestBuilders.bridge;
import static in.projecteka.gateway.registry.TestBuilders.bridgeRegistryRequest;
import static in.projecteka.gateway.registry.TestBuilders.bridgeService;
import static in.projecteka.gateway.registry.TestBuilders.bridgeServiceRequest;
import static in.projecteka.gateway.registry.TestBuilders.cmServiceRequest;
import static in.projecteka.gateway.registry.TestBuilders.facilityByIDResponseBuilder;
import static in.projecteka.gateway.registry.TestBuilders.facilitySearchResponseBuilder;
import static in.projecteka.gateway.registry.TestBuilders.hfrBridgeResponse;
import static in.projecteka.gateway.registry.TestBuilders.hfrFacilityRepresentationBuilder;
import static in.projecteka.gateway.registry.TestBuilders.realmRole;
import static in.projecteka.gateway.registry.TestBuilders.serviceAccount;
import static in.projecteka.gateway.registry.TestBuilders.serviceProfile;
import static in.projecteka.gateway.registry.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestBuilders.clientSecret;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    FacilityRegistryClient facilityRegistryClient;

    @BeforeEach
    void init() {
        initMocks(this);
        registryService = Mockito.spy(new RegistryService(
                registryRepository, consentManagerMappings, bridgeMappings, adminServiceClient, facilityRegistryClient
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
        var bridgeService = bridgeService().build();
        when(registryRepository.ifPresent(bridgeId)).thenReturn(just(bridge));
        when(registryRepository.updateBridgeEntry(request)).thenReturn(empty());
        when(registryRepository.fetchBridgeServicesIfPresent(bridgeId)).thenReturn(Flux.just(bridgeService));
        when(bridgeMappings.invalidate(Pair.of(bridgeService.getId(), bridgeService.getType()))).thenReturn(empty());
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
        when(registryRepository.fetchBridgeServicesIfPresent(bridgeId)).thenReturn(Flux.empty());
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
        when(registryRepository.ifBridgeServicePresent(bridgeId, request.getId())).thenReturn(just(false));
        when(registryRepository.insertBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any())).thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifBridgeServicePresent(bridgeId, request.getId());
        verify(registryRepository).insertBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any());
    }

    @Test
    void shouldUpdateBridgeServiceEntries() {
        var request = bridgeServiceRequest().active(false).build();
        var bridgeId = string();
        when(registryRepository.ifBridgeServicePresent(bridgeId, request.getId())).thenReturn(just(true));
        when(registryRepository.updateBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any())).thenReturn(empty());
        when(bridgeMappings.invalidate(Pair.of(request.getId(), request.getType()))).thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifBridgeServicePresent(bridgeId, request.getId());
        verify(registryRepository).updateBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any());
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
        when(registryRepository.ifBridgeServicePresent(bridgeId, request.getId())).thenReturn(just(false));
        when(registryRepository.insertBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any()))
                .thenReturn(empty());
        when(adminServiceClient.getServiceAccount(bridgeId)).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId);
        verify(registryRepository).ifBridgeServicePresent(bridgeId, request.getId());
        verify(registryRepository).insertBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any());
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
        when(registryRepository.ifBridgeServicePresent(bridgeId, request.getId())).thenReturn(just(true));
        when(registryRepository.updateBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any()))
                .thenReturn(empty());
        when(bridgeMappings.invalidate(Pair.of(request.getId(), request.getType()))).thenReturn(empty());
        when(adminServiceClient.getServiceAccount(bridgeId)).thenReturn(just(serviceAccount));
        when(adminServiceClient.getAvailableRealmRoles(serviceAccount.getId())).thenReturn(just(realmRoles));
        when(adminServiceClient.assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId()))
                .thenReturn(empty());

        var producer = registryService.populateBridgeServicesEntries(bridgeId, List.of(request));
        StepVerifier.create(producer)
                .verifyComplete();

        verify(registryRepository).ifPresent(request.getId(), request.getType(), request.isActive(), bridgeId);
        verify(registryRepository).ifBridgeServicePresent(bridgeId, request.getId());
        verify(registryRepository).updateBridgeServiceEntry(eq(bridgeId), eq(request.getId()), eq(request.getName()), serviceDetails.getEndpoints(), any());
        verify(bridgeMappings).invalidate(Pair.of(request.getId(), request.getType()));
        verify(adminServiceClient).getServiceAccount(bridgeId);
        verify(adminServiceClient).getAvailableRealmRoles(serviceAccount.getId());
        verify(adminServiceClient).assignRoleToClient(List.of(realmRoles.get(0)), serviceAccount.getId());
    }

    @Test
    void shouldReturnServiceProfileForGivenServiceId() {
        var serviceId = string();
        var serviceProfile = serviceProfile().build();
        when(registryRepository.fetchServiceEntries(serviceId)).thenReturn(Mono.just(serviceProfile));

        var profileProducer = registryService.serviceProfile(serviceId);
        StepVerifier.create(profileProducer)
                .assertNext(response -> assertThat(response).isInstanceOf(ServiceProfileResponse.class))
                .verifyComplete();

        verify(registryRepository).fetchServiceEntries(serviceId);
    }

    @Test
    void shouldThrowNotFoundIfServiceIdIsNotPresent() {
        var serviceId = string();
        when(registryRepository.fetchServiceEntries(serviceId)).thenReturn(Mono.empty());

        var profileProducer = registryService.serviceProfile(serviceId);
        StepVerifier.create(profileProducer)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus() == HttpStatus.NOT_FOUND);

        verify(registryRepository).fetchServiceEntries(serviceId);
    }

    @Test
    void shouldReturnBridgeProfileForValidBridgeId() {
        var bridgeId = string();
        var bridgeProfileResponse = hfrBridgeResponse().id(bridgeId).build();
        when(registryRepository.bridgeProfile(bridgeId)).thenReturn(Mono.just(bridgeProfileResponse));

        StepVerifier.create(registryService.bridgeProfile(bridgeId))
                .expectNext(bridgeProfileResponse)
                .verifyComplete();
    }

    @Test
    void shouldThrowNotFoundForInvalidBridgeId() {
        var bridgeId = string();
        when(registryRepository.bridgeProfile(bridgeId)).thenReturn(Mono.empty());

        StepVerifier.create(registryService.bridgeProfile(bridgeId))
                .expectErrorMatches(throwable -> throwable instanceof ClientError
                        && ((ClientError) throwable).getHttpStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void shouldReturnEmptyListIfFacilityNameIsNotProvided() {
        var name = "";
        var state = string();
        var district = string();

        StepVerifier.create(registryService.searchFacilityByName(name, state, district))
                .expectNext(List.of())
                .verifyComplete();
    }

    @Test
    void shouldReturnIsHIPAsTrueIfTheSearchedFacilityIsRegisteredAsHIPWhenSearchingFacility() {
        var name = string();
        var state = string();
        var district = string();

        var hfrFacility = hfrFacilityRepresentationBuilder().active("Y").build();
        var facilitySearchResponse = facilitySearchResponseBuilder().facilities(List.of(hfrFacility)).build();
        var serviceProfile = serviceProfile().types(List.of(HIP)).build();

        when(facilityRegistryClient.searchFacilityByName(eq(name), eq(state), eq(district))).thenReturn(Mono.just(facilitySearchResponse));
        when(registryRepository.fetchServiceEntries(eq(hfrFacility.getId()))).thenReturn(Mono.just(serviceProfile));

        StepVerifier.create(registryService.searchFacilityByName(name, state, district))
                .expectNextMatches(facilities -> {
                    var facility = facilities.get(0);
                    return facility.getIsHIP() && facility.getIdentifier().getId().equals(hfrFacility.getId());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnIsHIPAsFalseIfTheSearchedFacilityIsNotRegisteredAsHIPWhenSearchingFacility() {
        var name = string();
        var state = string();
        var district = string();

        var hfrFacility = hfrFacilityRepresentationBuilder().active("Y").build();
        var facilitySearchResponse = facilitySearchResponseBuilder().facilities(List.of(hfrFacility)).build();
        var serviceProfile = serviceProfile().types(List.of(HIU)).build();

        when(facilityRegistryClient.searchFacilityByName(eq(name), eq(state), eq(district))).thenReturn(Mono.just(facilitySearchResponse));
        when(registryRepository.fetchServiceEntries(eq(hfrFacility.getId()))).thenReturn(Mono.just(serviceProfile));

        StepVerifier.create(registryService.searchFacilityByName(name, state, district))
                .expectNextMatches(facilities -> {
                    var facility = facilities.get(0);
                    return facility.getIsHIP().equals(false) && facility.getIdentifier().getId().equals(hfrFacility.getId());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnIsHIPAsFalseIfTheSearchedFacilityIsNotRegisteredOnGatewayWhenSearchingFacility() {
        var name = string();
        var state = string();
        var district = string();

        var hfrFacility = hfrFacilityRepresentationBuilder().build();
        var facilitySearchResponse = facilitySearchResponseBuilder().facilities(List.of(hfrFacility)).build();

        when(facilityRegistryClient.searchFacilityByName(eq(name), eq(state), eq(district))).thenReturn(Mono.just(facilitySearchResponse));
        when(registryRepository.fetchServiceEntries(eq(hfrFacility.getId()))).thenReturn(Mono.empty());

        StepVerifier.create(registryService.searchFacilityByName(name, state, district))
                .expectNextMatches(facilities -> {
                    var facility = facilities.get(0);
                    return facility.getIsHIP().equals(false) && facility.getIdentifier().getId().equals(hfrFacility.getId());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnIsHIPAsFalseIfTheSearchedFacilityIsNotActiveWhenSearchingFacility() {
        var name = string();
        var state = string();
        var district = string();

        var hfrFacility = hfrFacilityRepresentationBuilder().active("N").build();
        var facilitySearchResponse = facilitySearchResponseBuilder().facilities(List.of(hfrFacility)).build();
        var serviceProfile = serviceProfile().types(List.of(HIP)).build();

        when(facilityRegistryClient.searchFacilityByName(eq(name), eq(state), eq(district))).thenReturn(Mono.just(facilitySearchResponse));
        when(registryRepository.fetchServiceEntries(eq(hfrFacility.getId()))).thenReturn(Mono.just(serviceProfile));

        StepVerifier.create(registryService.searchFacilityByName(name, state, district))
                .expectNextMatches(facilities -> {
                    var facility = facilities.get(0);
                    return facility.getIsHIP().equals(false) && facility.getIdentifier().getId().equals(hfrFacility.getId());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnIsHIPAsFalseIfTheFacilityIsNotRegisteredOnGatewayWhenFetchingFacilityById() {
        var facilityId = string();

        var facilityByIdResponse = facilityByIDResponseBuilder().build();
        var hfrFacility = facilityByIdResponse.getFacility();

        when(facilityRegistryClient.getFacilityById(eq(facilityId))).thenReturn(Mono.just(facilityByIdResponse));
        when(registryRepository.fetchServiceEntries(eq(hfrFacility.getId()))).thenReturn(Mono.empty());

        StepVerifier.create(registryService.getFacilityById(facilityId))
                .expectNextMatches(facility ->
                        facility.getIsHIP().equals(false) && facility.getIdentifier().getId().equals(hfrFacility.getId())
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnIsHIPAsFalseIfTheFacilityIsNotRegisteredAsHIPWhenFetchingFacilityById() {
        var facilityId = string();

        var hfrFacility = hfrFacilityRepresentationBuilder().active("Y").build();
        var facilityByIdResponse = facilityByIDResponseBuilder().facility(hfrFacility).build();
        var serviceProfile = serviceProfile().types(List.of(HIU)).build();

        when(facilityRegistryClient.getFacilityById(eq(facilityId))).thenReturn(Mono.just(facilityByIdResponse));
        when(registryRepository.fetchServiceEntries(eq(hfrFacility.getId()))).thenReturn(Mono.just(serviceProfile));

        StepVerifier.create(registryService.getFacilityById(facilityId))
                .expectNextMatches(facility ->
                        facility.getIsHIP().equals(false) && facility.getIdentifier().getId().equals(hfrFacility.getId())
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnIsHIPAsFalseIfTheFacilityIsRegisteredAsHIPWhenFetchingFacilityById() {
        var facilityId = string();

        var hfrFacility = hfrFacilityRepresentationBuilder().active("Y").build();
        var facilityByIdResponse = facilityByIDResponseBuilder().facility(hfrFacility).build();
        var serviceProfile = serviceProfile().types(List.of(HIP)).build();

        when(facilityRegistryClient.getFacilityById(eq(facilityId))).thenReturn(Mono.just(facilityByIdResponse));
        when(registryRepository.fetchServiceEntries(eq(hfrFacility.getId()))).thenReturn(Mono.just(serviceProfile));

        StepVerifier.create(registryService.getFacilityById(facilityId))
                .expectNextMatches(facility ->
                        facility.getIsHIP() && facility.getIdentifier().getId().equals(hfrFacility.getId())
                )
                .verifyComplete();
    }

    @Test
    void shouldThrowErrorIfFacilityNotFoundWhenFetchingFacilityById() {
        var facilityId = string();

        var hfrFacility = hfrFacilityRepresentationBuilder().id(null).build();
        var facilityByIdResponse = facilityByIDResponseBuilder()
                .facility(hfrFacility)
                .build();

        when(facilityRegistryClient.getFacilityById(eq(facilityId))).thenReturn(Mono.just(facilityByIdResponse));

        StepVerifier.create(registryService.getFacilityById(facilityId))
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus() == HttpStatus.NOT_FOUND);
    }
}
