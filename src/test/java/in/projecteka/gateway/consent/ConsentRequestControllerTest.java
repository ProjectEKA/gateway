package in.projecteka.gateway.consent;

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
public class ConsentRequestControllerTest {
    @MockBean
    RequestOrchestrator<ConsentRequestServiceClient> requestOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void shouldFireAndForgetForConsentRequestInit() {
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
}
