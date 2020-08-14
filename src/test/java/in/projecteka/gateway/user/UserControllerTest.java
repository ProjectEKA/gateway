package in.projecteka.gateway.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.AuthConfirmServiceClient;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.ValidatedResponseAction;
import in.projecteka.gateway.common.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;


import static in.projecteka.gateway.common.Constants.BRIDGE_ID_PREFIX;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIP;
import static in.projecteka.gateway.common.Role.HIU;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
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

    @MockBean
    RequestOrchestrator<AuthConfirmServiceClient> authConfirmRequestOrchestrator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    Authenticator authenticator;

    @MockBean
    @Qualifier("patientSearchResponseAction")
    ValidatedResponseAction validatedResponseAction;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @Qualifier("patientSearchResponseOrchestrator")
    @MockBean
    ResponseOrchestrator patientSearchResponseOrchestrator;

    @Qualifier("authConfirmResponseOrchestrator")
    @MockBean
    ResponseOrchestrator authConfirmResponseOrchestrator;

    @MockBean
    Validator patientSearchValidator;

    @Test
    void shouldFireAndForgetForPatientsFindInUserController() {
        var token = string();
        var clientId = string();
        when(patientSearchOrchestrator
                .handleThis(any(), eq(X_CM_ID), eq(X_HIU_ID), eq(BRIDGE_ID_PREFIX + clientId)))
                .thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIU)).build()));

        webTestClient
                .post()
                .uri(Constants.PATH_PATIENTS_FIND)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForPatientOnFind() throws JsonProcessingException {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(patientSearchResponseOrchestrator.processResponse(any(),eq(X_HIU_ID))).thenReturn(empty());
        webTestClient
                .post()
                .uri(Constants.PATH_PATIENTS_ON_FIND)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForAuthConfirmHipInitiatedLinking() throws JsonProcessingException {
        var token = string();
        var clientId = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIP)).build()));
        when(authConfirmRequestOrchestrator
                .handleThis(any(), eq(X_CM_ID), eq(X_HIP_ID), eq(BRIDGE_ID_PREFIX + clientId)))
                .thenReturn(empty());
        webTestClient
                .post()
                .uri(Constants.USERS_AUTH_CONFIRM)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
    @Test
    void shouldFireAndForgetForAuthOnConfirmHipInitiatedLinking() throws JsonProcessingException {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(authConfirmResponseOrchestrator.processResponse(any(), eq(X_HIP_ID))).thenReturn(empty());
        webTestClient
                .post()
                .uri(Constants.USERS_AUTH_ON_CONFIRM)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}
