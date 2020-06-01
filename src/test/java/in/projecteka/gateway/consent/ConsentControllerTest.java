package in.projecteka.gateway.consent;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.ConsentFetchServiceClient;
import in.projecteka.gateway.clients.ConsentRequestServiceClient;
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

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentControllerTest {
    @MockBean
    RequestOrchestrator<ConsentRequestServiceClient> requestOrchestrator;

    @MockBean
    RequestOrchestrator<ConsentFetchServiceClient> consentFetchOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @Test
    void shouldFireAndForgetForConsentRequestInit() {
        Mockito.when(requestOrchestrator.processRequest(Mockito.any(), eq(X_CM_ID))).thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/consent-requests/init")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void shouldFireAndForgetForConsentFetch() {
        Mockito.when(consentFetchOrchestrator.processRequest(Mockito.any(), eq(X_CM_ID))).thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/consents/fetch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }
}
