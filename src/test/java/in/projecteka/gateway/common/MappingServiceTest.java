package in.projecteka.gateway.common;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.common.model.ServiceProperties;
import in.projecteka.gateway.common.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "6000")
class MappingServiceTest {
    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private MappingRepository mappingRepository;

    MappingService mappingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mappingService = Mockito.spy(new MappingService( mappingRepository));
    }

    @Test
    void shouldDependentSystemUrls(){
        Flux<ServiceProperties> bridgePropertiesFlux = Flux.just(ServiceProperties
                        .builder()
                        .name(string())
                        .id(string())
                        .url(string())
                        .build(),
                ServiceProperties
                        .builder()
                        .name(string())
                        .id(string())
                        .url(string())
                        .build());
        Flux<ServiceProperties> consentManagerPropertiesFlux = Flux.just(ServiceProperties.builder()
                        .name(string())
                        .id(string())
                        .url(string())
                        .build());
        when(mappingRepository.selectBridgeProperties()).thenReturn(bridgePropertiesFlux);
        when(mappingRepository.selectConsentManagerProperties()).thenReturn(consentManagerPropertiesFlux);

        Mono<Service> allUrls = mappingService.fetchDependentServiceUrls();

        assertThat(allUrls.block().getBridgeProperties())
                .hasSize(2);
        assertThat(allUrls.block().getConsentManagerProperties())
                .hasSize(1);
    }
}
