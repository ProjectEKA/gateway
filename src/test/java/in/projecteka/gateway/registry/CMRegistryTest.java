package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CMRegistryTest {

    @Mock
    @Qualifier("consentManagerMappings")
    CacheAdapter<String, String> consentManagerMappings;

    @Mock
    MappingRepository mappingRepository;

    CMRegistry cmRegistry;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        cmRegistry = Mockito.spy(new CMRegistry(consentManagerMappings, mappingRepository));
    }

    @Test
    void returnHostByHittingDBWhenCacheDoesnotHoldRequestedMappingInfo() {
        var clientId = string();
        var url = string();
        when(consentManagerMappings.get(clientId)).thenReturn(Mono.empty());
        when(mappingRepository.cmHost(clientId)).thenReturn(Mono.just(url));
        when(consentManagerMappings.put(clientId, url)).thenReturn(Mono.empty());

        var mayBeHost = cmRegistry.getHostFor(clientId);

        assertThat(mayBeHost).isNotNull();
    }

    @Test
    void returnHostFromCacheWhenItContainsRequestedMappingInfo() {
        var clientId = string();
        var url = string();
        when(consentManagerMappings.get(clientId)).thenReturn(Mono.just(url));
        when(mappingRepository.cmHost(clientId)).thenReturn(Mono.empty());

        var mayBeHost = cmRegistry.getHostFor(clientId);

        assertThat(mayBeHost).isNotNull();
    }
}