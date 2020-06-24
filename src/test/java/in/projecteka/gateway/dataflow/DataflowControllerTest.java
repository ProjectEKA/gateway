package in.projecteka.gateway.dataflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.DataFlowRequestServiceClient;
import in.projecteka.gateway.clients.HealthInfoNotificationServiceClient;
import in.projecteka.gateway.clients.HipDataFlowServiceClient;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static in.projecteka.gateway.common.Constants.V_1_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static in.projecteka.gateway.common.Constants.REQUEST_ID;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIP;
import static in.projecteka.gateway.common.Role.HIU;
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
class DataflowControllerTest {
    @MockBean
    RequestOrchestrator<DataFlowRequestServiceClient> dataFlowRequestOrchestrator;

    @MockBean
    RequestOrchestrator<HipDataFlowServiceClient> hipDataFlowRequestOrchestrator;

    @MockBean
    RequestOrchestrator<HealthInfoNotificationServiceClient> healthInfoNotificationOrchestrator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    Authenticator authenticator;

    @Qualifier("dataFlowRequestResponseOrchestrator")
    @MockBean
    ResponseOrchestrator dataFlowRequestResponseOrchestrator;

    @MockBean
    @Qualifier("dataFlowRequestResponseAction")
    ValidatedResponseAction validatedResponseAction;

    @Qualifier("hipDataFlowRequestResponseOrchestrator")
    @MockBean
    ResponseOrchestrator hipDataFlowRequestResponseOrchestrator;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @MockBean
    Validator dataFlowResposeValidator;

    @Test
    void shouldFireAndForgetForInitDataFlowRequest() {
        var token = string();
        var clientId = string();
        when(dataFlowRequestOrchestrator.handleThis(any(), eq(X_CM_ID), eq(clientId))).thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIU)).build()));

        webTestClient
                .post()
                .uri("/v1/health-information/cm/request")
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
    @Test
    void shouldFireAndForgetForOnInitDataFlowRequest() throws IOException {
        var requestId = UUID.randomUUID().toString();
        var callerRequestId = UUID.randomUUID().toString();
        var objectNode = OBJECT_MAPPER.createObjectNode();
        var respNode = OBJECT_MAPPER.createObjectNode();
        var token = string();
        var testId = string();
        objectNode.put(REQUEST_ID, requestId);
        respNode.put(REQUEST_ID, callerRequestId);
        objectNode.set("resp", respNode);
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(objectNode));

        when(dataFlowResposeValidator.validateResponse(requestEntity, X_HIU_ID))
                .thenReturn(just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(testId), jsonNodeArgumentCaptor.capture()))
                .thenReturn(empty());
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));

        webTestClient
                .post()
                .uri("/v1/health-information/cm/on-request")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForHipDataFlowRequestInDataFlowController() {
        var token = string();
        var clientId = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(CM)).build()));
        when(hipDataFlowRequestOrchestrator.handleThis(any(), eq(X_HIP_ID), eq(clientId))).thenReturn(empty());

        webTestClient
                .post()
                .uri("/v1/health-information/hip/request")
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetHealthInfoNotification() {
        var token = string();
        var clientId = string();
        when(healthInfoNotificationOrchestrator.handleThis(any(), eq(X_CM_ID), eq(clientId))).thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIU, HIP)).build()));

        webTestClient
                .post()
                .uri(V_1_HEALTH_INFORMATION_NOTIFY)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForHipDataFlowResponse() throws JsonProcessingException {
        var token = string();
        var clientId = string();
        var requestId = UUID.randomUUID().toString();
        var callerRequestId = UUID.randomUUID().toString();
        var testId = string();
        var objectNode = OBJECT_MAPPER.createObjectNode();
        var respNode = OBJECT_MAPPER.createObjectNode();
        objectNode.put(REQUEST_ID, requestId);
        respNode.put(REQUEST_ID, callerRequestId);
        objectNode.set("resp", respNode);
        var requestEntity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(objectNode));

        when(dataFlowResposeValidator.validateResponse(requestEntity, X_CM_ID))
                .thenReturn(just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(testId), jsonNodeArgumentCaptor.capture()))
                .thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIP)).build()));

        webTestClient
                .post()
                .uri("/v1/health-information/hip/on-request")
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}