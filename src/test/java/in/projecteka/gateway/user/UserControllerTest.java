package in.projecteka.gateway.user;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
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
class UserControllerTest {

    @MockBean
    RequestOrchestrator<PatientSearchServiceClient> patientSearchOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;


    @Test
    void shouldFireAndForgetForConsentFetch() {
        Mockito.when(patientSearchOrchestrator.processRequest(Mockito.any(), eq(X_CM_ID))).thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/patients/find")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }

}