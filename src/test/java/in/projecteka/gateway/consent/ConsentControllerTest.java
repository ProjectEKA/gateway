package in.projecteka.gateway.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.ConsentFetchServiceClient;
import in.projecteka.gateway.clients.ConsentRequestServiceClient;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.ValidatedResponse;
import in.projecteka.gateway.common.ValidatedResponseAction;
import in.projecteka.gateway.common.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentControllerTest {
    @MockBean
    RequestOrchestrator<ConsentRequestServiceClient> requestOrchestrator;

    @MockBean
    RequestOrchestrator<ConsentFetchServiceClient> consentFetchOrchestrator;

    @Qualifier("consentResponseOrchestrator")
    @MockBean
    ResponseOrchestrator consentResponseOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @MockBean
    Validator consentRequestValidator;

    @MockBean
    @Qualifier("consentResponseAction")
    ValidatedResponseAction validatedResponseAction;

    private @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

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
    void shouldFireAndForgetForConsentRequestOnInit() throws JsonProcessingException {

        String requestId = UUID.randomUUID().toString();
        String callerRequestId = UUID.randomUUID().toString();
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("requestId",requestId);
        ObjectNode respNode = new ObjectMapper().createObjectNode();
        respNode.put("requestId",callerRequestId);
        objectNode.set("resp",respNode);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(objectNode));

        String testId = "testId";
        when(consentRequestValidator.validateResponse(requestEntity, X_HIU_ID)).thenReturn(Mono.just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(X_HIU_ID), eq(testId), jsonNodeArgumentCaptor.capture())).thenReturn(Mono.empty());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/consent-requests/on-init")
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
