package in.projecteka.gateway.consent;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.ConsentArtefactServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConsentArtefactControllerTest {
    @MockBean
    RequestOrchestrator<ConsentArtefactServiceClient> consentArtefactHipNotifyOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @Test
    public void shouldFireAndForgetHIPConsentNotification() {
        Mockito.when(consentArtefactHipNotifyOrchestrator.processRequest(Mockito.any(), eq(X_HIP_ID)))
                .thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/consents/hip/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }
}
