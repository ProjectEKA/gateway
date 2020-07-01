package in.projecteka.gateway.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.HipConsentNotifyServiceClient;
import in.projecteka.gateway.clients.HiuConsentNotifyServiceClient;
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

import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIP;
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
public class ConsentArtefactControllerTest {
    @MockBean
    RequestOrchestrator<HipConsentNotifyServiceClient> hipConsentNotifyRequestOrchestrator;

    @MockBean
    RequestOrchestrator<HiuConsentNotifyServiceClient> hiuConsentNotifyRequestOrchestrator;

    @Qualifier("hipConsentNotifyResponseOrchestrator")
    @MockBean
    ResponseOrchestrator hipConsentNotifyResponseOrchestrator;

    @MockBean
    Validator notifyValidator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    @Qualifier("hipConsentNotifyResponseAction")
    ValidatedResponseAction validatedResponseAction;

    @MockBean
    Authenticator authenticator;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @Test
    public void shouldFireAndForgetHIPConsentNotification() {
        var token = string();
        var clientId = string();
        when(hipConsentNotifyRequestOrchestrator.handleThis(any(), eq(X_HIP_ID), eq(X_CM_ID), eq(clientId)))
                .thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(CM)).build()));

        webTestClient
                .post()
                .uri("/v1/consents/hip/notify")
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    public void shouldFireAndForgetHIUConsentNotification() {
        var clientId = string();
        var token = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(CM)).build()));
        when(hiuConsentNotifyRequestOrchestrator.handleThis(any(), eq(X_HIU_ID), eq(X_CM_ID), eq(clientId)))
                .thenReturn(empty());

        webTestClient
                .post()
                .uri("/v1/consents/hiu/notify")
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    public void shouldFireAndForgetForOnDiscover() throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        var callerRequestId = UUID.randomUUID().toString();
        var objectNode = OBJECT_MAPPER.createObjectNode();
        var respNode = OBJECT_MAPPER.createObjectNode();
        var token = string();
        var testId = string();
        objectNode.put(REQUEST_ID, requestId);
        respNode.put(REQUEST_ID, callerRequestId);
        objectNode.set("resp", respNode);
        var routingKey = X_CM_ID;
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(objectNode));
        when(notifyValidator.validateResponse(requestEntity, routingKey))
                .thenReturn(just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(testId), jsonNodeArgumentCaptor.capture(), eq(routingKey)))
                .thenReturn(empty());
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(HIP)).build()));

        webTestClient
                .post()
                .uri("/v1/consents/hip/on-notify")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}
