package in.projecteka.gateway.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.PatientSearchServiceClient;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.ValidatedResponse;
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
import org.springframework.http.HttpEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import java.util.UUID;

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIU;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.testcommon.TestBuilders.caller;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
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

    @MockBean
    Validator patientSearchValidator;

    @Test
    void shouldFireAndForgetForPatientsFindInUserController() {
        var token = string();
        var clientId = string();
        when(patientSearchOrchestrator.handleThis(any(), eq(X_CM_ID), eq(X_HIU_ID), eq(clientId))).thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIU)).build()));

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

    @Test
    void shouldFireAndForgetForPatientOnFind() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        var callerRequestId = UUID.randomUUID().toString();
        var objectNode = OBJECT_MAPPER.createObjectNode();
        var testId = string();
        var token = string();
        var routingKey = X_HIU_ID;
        objectNode.put(REQUEST_ID, requestId);
        ObjectNode respNode = OBJECT_MAPPER.createObjectNode();
        respNode.put(REQUEST_ID, callerRequestId);
        objectNode.set("resp", respNode);
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(objectNode));
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(patientSearchValidator.validateResponse(requestEntity, routingKey))
                .thenReturn(just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(testId), jsonNodeArgumentCaptor.capture(), eq(routingKey)))
                .thenReturn(empty());

        webTestClient
                .post()
                .uri("/v1/patients/on-find")
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}