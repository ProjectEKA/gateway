package in.projecteka.gateway.dataflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.DataFlowRequestServiceClient;
import in.projecteka.gateway.clients.HealthInfoNotificationServiceClient;
import in.projecteka.gateway.clients.HipDataFlowServiceClient;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.ValidatedResponse;
import in.projecteka.gateway.common.ValidatedResponseAction;
import in.projecteka.gateway.common.Validator;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.UUID;

import static in.projecteka.gateway.common.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    ResponseOrchestrator hipDataFlowRequestResponseOrchestrator;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @MockBean
    Validator dataFlowResponseValidator;

    @BeforeEach
    void init() {
        hipDataFlowRequestResponseOrchestrator = new ResponseOrchestrator(dataFlowResponseValidator,
                validatedResponseAction);
    }

    @Test
    void shouldFireAndForgetForInitDataFlowRequest() {
        var token = string();
        var clientId = string();
        when(dataFlowRequestOrchestrator.handleThis(any(), eq(X_CM_ID), eq(X_HIU_ID), eq(clientId))).thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIU)).build()));

        webTestClient
                .post()
                .uri(Constants.PATH_HEALTH_INFORMATION_CM_REQUEST)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForOnInitDataFlowRequest() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(dataFlowRequestResponseOrchestrator.processResponse(any(), eq(X_HIU_ID)))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.PATH_HEALTH_INFORMATION_CM_ON_REQUEST)
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
        when(hipDataFlowRequestOrchestrator.handleThis(any(), eq(X_HIP_ID), eq(X_CM_ID), eq(clientId)))
                .thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_HEALTH_INFORMATION_HIP_REQUEST)
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
        when(healthInfoNotificationOrchestrator.handleThis(any(), eq(X_CM_ID), anyString(), eq(clientId)))
                .thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIU, HIP)).build()));

        webTestClient
                .post()
                .uri(PATH_HEALTH_INFORMATION_NOTIFY)
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
        when(dataFlowResponseValidator.validateResponse(any(), eq(X_CM_ID)))
                .thenReturn(just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(testId), jsonNodeArgumentCaptor.capture(), eq(X_CM_ID)))
                .thenReturn(empty());
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIP)).build()));

        webTestClient
                .post()
                .uri(Constants.PATH_HEALTH_INFORMATION_HIP_ON_REQUEST)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}