package in.projecteka.gateway.user;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
import in.projecteka.gateway.common.CentralRegistryTokenVerifier;
import in.projecteka.gateway.common.RequestOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.caller;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class UserControllerTest {

    @MockBean
    RequestOrchestrator<PatientSearchServiceClient> patientSearchOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    CentralRegistryTokenVerifier centralRegistryTokenVerifier;


    @Test
    void shouldFireAndForgetForPatientsFind() {
        var token = string();
        var clientId = string();
        when(patientSearchOrchestrator.processRequest(any(), eq(X_CM_ID), eq(clientId))).thenReturn(empty());
        when(centralRegistryTokenVerifier.verify(token)).thenReturn(just(caller().clientId(clientId).build()));

        webTestClient
                .post()
                .uri("/v1/patients/find")
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

}