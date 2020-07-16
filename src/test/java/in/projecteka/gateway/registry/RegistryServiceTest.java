package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.cache.CacheAdapter;
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

class RegistryServiceTest {

    @Mock
    @Qualifier("consentManagerMappings")
    CacheAdapter<String, String> consentManagerMappings;

    @Mock
    private RegistryRepository registryRepository;

    RegistryService registryService;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void shouldInsertCMEntryIfItDoesNotExist() {
        registryService = new RegistryService(registryRepository, consentManagerMappings);
        var request = cmServiceRequest().build();

        when(registryRepository.getCMEntryCount(request.getCmSuffix())).thenReturn(Mono.just(0));
        when(registryRepository.createCMEntry(request)).thenReturn(Mono.empty());

        StepVerifier.create(registryService.populateCMEntry(request)).verifyComplete();

        verify(registryRepository, times(1)).getCMEntryCount(request.getCmSuffix());
        verify(registryRepository, times(1)).createCMEntry(request);
    }

    @Test
    void shouldUpdateCMEntryIfItExists() {
        RegistryService registryService = new RegistryService(registryRepository, consentManagerMappings);
        var request = cmServiceRequest().build();

        when(registryRepository.getCMEntryCount(request.getCmSuffix())).thenReturn(Mono.just(1));
        when(registryRepository.updateCMEntry(request)).thenReturn(Mono.create(MonoSink::success));
        when(consentManagerMappings.invalidate(request.getCmSuffix())).thenReturn(Mono.empty());

        StepVerifier.create(registryService.populateCMEntry(request)).verifyComplete();

        verify(registryRepository, times(1)).getCMEntryCount(request.getCmSuffix());
        verify(registryRepository, times(1)).updateCMEntry(request);
        verify(consentManagerMappings).invalidate(request.getCmSuffix());
    }

}