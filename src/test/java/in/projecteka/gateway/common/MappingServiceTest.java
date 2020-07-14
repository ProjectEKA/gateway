package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.common.model.Path;
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
    void shouldGiveUrls() throws JsonProcessingException {
        Flux<String> bridgeUrls = Flux.just("http://localhost:9052", "http://localhost:8001", "http://localhost:8003");
        when(mappingRepository.selectBridgeUrls()).thenReturn(bridgeUrls);

        Mono<Path> allUrls = mappingService.getAllUrls();

        assertThat(allUrls.block().getBridgeUrls())
                .hasSize(3)
                .contains("http://localhost:9052", "http://localhost:8001", "http://localhost:8003");
    }
}
