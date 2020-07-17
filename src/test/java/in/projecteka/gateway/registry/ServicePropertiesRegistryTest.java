package in.projecteka.gateway.registry;

import in.projecteka.gateway.common.MappingRepository;
import in.projecteka.gateway.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ServicePropertiesRegistryTest {

    @Mock
    CacheAdapter<Pair<String, ServiceType>, String> bridgeMappings;

    @Mock
    MappingRepository mappingRepository;

    BridgeRegistry bridgeRegistry;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        bridgeRegistry = Mockito.spy(new BridgeRegistry(bridgeMappings, mappingRepository));
    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, names = {"HIP", "HIU"})
    void returnHostByHittingDBWhenCacheDoesnotHoldRequestedMappingInfo(ServiceType serviceType) {
        var clientId = string();
        var url = string();
        var key = Pair.of(clientId, serviceType);
        when(bridgeMappings.get(key)).thenReturn(Mono.empty());
        when(mappingRepository.bridgeHost(key)).thenReturn(Mono.just(url));
        when(bridgeMappings.put(key, url)).thenReturn(Mono.empty());

        var mayBeHost = bridgeRegistry.getHostFor(clientId, serviceType);

        assertThat(mayBeHost).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, names = {"HIP", "HIU"})
    void returnHostFromCacheWhenItContainsRequestedMappingInfo(ServiceType serviceType) {
        var clientId = string();
        var url = string();
        var key = Pair.of(clientId, serviceType);
        when(bridgeMappings.get(key)).thenReturn(Mono.just(url));
        when(mappingRepository.bridgeHost(key)).thenReturn(Mono.empty());

        var mayBeHost = bridgeRegistry.getHostFor(clientId, serviceType);

        assertThat(mayBeHost).isNotNull();
    }
}
