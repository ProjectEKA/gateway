package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.common.model.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "6000")
class MappingControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldGiveUrls() throws JsonProcessingException {
        List<String> urls=List.of("http://localhost:9052","http://localhost:8001","http://localhost:8003");
        Path pathUrls = Path.builder().bridgeUrls(urls).build();

        var bridgeUrlsJson = TestBuilders.OBJECT_MAPPER.writeValueAsString(pathUrls);
        when(mappingService.getAllUrls()).thenReturn(Mono.just(pathUrls));

        webTestClient.get()
                .uri(Constants.GET_BRIDGE_URLS)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(bridgeUrlsJson);
    }
}
