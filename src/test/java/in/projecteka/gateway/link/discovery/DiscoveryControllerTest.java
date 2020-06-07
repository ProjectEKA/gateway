package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.common.CentralRegistryTokenVerifier;
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

import java.util.UUID;

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
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
class DiscoveryControllerTest {
    @MockBean
    RequestOrchestrator<DiscoveryServiceClient> requestOrchestrator;

    @Qualifier("discoveryResponseOrchestrator")
    @MockBean
    ResponseOrchestrator discoveryResponseOrchestrator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    Validator discoveryValidator;

    @MockBean
    @Qualifier("discoveryResponseAction")
    ValidatedResponseAction validatedResponseAction;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @MockBean
    CentralRegistryTokenVerifier centralRegistryTokenVerifier;

    @Test
    public void shouldFireAndForgetForDiscover() {
        var token = string();
        var clientId = string();
        when(requestOrchestrator.processRequest(any(), eq(X_HIP_ID), eq(clientId))).thenReturn(empty());
        when(centralRegistryTokenVerifier.verify(token)).thenReturn(just(caller().clientId(clientId).build()));

        webTestClient
                .post()
                .uri("/v1/care-contexts/discover")
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
        objectNode.put("requestId", requestId);
        respNode.put("requestId", callerRequestId);
        objectNode.set("resp", respNode);
        var requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(objectNode));
        when(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .thenReturn(just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(X_CM_ID), eq(testId), jsonNodeArgumentCaptor.capture()))
                .thenReturn(empty());
        when(centralRegistryTokenVerifier.verify(token)).thenReturn(just(caller().build()));

        webTestClient
                .post()
                .uri("/v1/care-contexts/on-discover")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}