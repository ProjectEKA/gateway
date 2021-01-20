package in.projecteka.gateway.patient;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.PatientSMSNotificationClient;
import in.projecteka.gateway.clients.PatientServiceClient;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.ShareProfile;
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
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.bridgeId;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIP;
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
public class PatientControllerTest {
    @MockBean
    RequestOrchestrator<PatientServiceClient> requestOrchestrator;

    @Qualifier("patientResponseOrchestrator")
    @MockBean
    ResponseOrchestrator patientResponseOrchestrator;

    @MockBean
    RequestOrchestrator<PatientSMSNotificationClient> patientSMSNotifyRequestOrchestrator;

    @MockBean
    @Qualifier("patientSMSNotifyResponseOrchestrator")
    ResponseOrchestrator patientSMSNotifyResponseOrchestrator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @MockBean
    Authenticator authenticator;

    @MockBean
    ShareProfile shareProfile;

    @Test
    void shouldFireAndForgetForPatientProfileShare() {
        var token = string();
        var clientId = string();
        when(requestOrchestrator.handleThis(any(), eq(X_HIP_ID), eq(X_CM_ID), eq(clientId))).thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(CM)).build()));
        when(shareProfile.isEnable()).thenReturn(Boolean.FALSE);

        webTestClient
                .post()
                .uri(Constants.PATH_PATIENT_SHARE)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void shouldFireAndForgetForPatientProfileOnShare() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(HIP)).build()));
        when(patientResponseOrchestrator.processResponse(any(), eq(X_CM_ID))).thenReturn(Mono.empty());
        when(shareProfile.isEnable()).thenReturn(Boolean.FALSE);

        webTestClient
                .post()
                .uri(Constants.PATH_PATIENT_ON_SHARE)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void shouldFireAndForgetForHIPSmsNotification() {
        var token = string();
        var clientId = string();
        when(patientSMSNotifyRequestOrchestrator.handleThis(any(), eq(X_CM_ID), eq(X_HIP_ID), eq(bridgeId(clientId)))).thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIP)).build()));

        webTestClient
                .post()
                .uri(Constants.PATH_PATIENTS_SMS_NOTIFY)
                .header(AUTHORIZATION, token)
                .header(X_CM_ID, "CM_ID")
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForHIPSmsNotificationResponse() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(patientSMSNotifyResponseOrchestrator.processResponse(any(), eq(X_HIP_ID))).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.PATH_PATIENTS_SMS_ON_NOTIFY)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}
