package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
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
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryControllerTest {
    @MockBean
    RequestOrchestrator<DiscoveryServiceClient> requestOrchestrator;

    @Qualifier("discoveryResponseOrchestrator")
    @MockBean
    ResponseOrchestrator discoveryResponseOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @MockBean
    Validator discoveryValidator;

    @MockBean
    @Qualifier("discoveryResponseAction")
    ValidatedResponseAction validatedResponseAction;

    private @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @Test
    public void shouldFireAndForgetForDiscover() {
        Mockito.when(requestOrchestrator.processRequest(Mockito.any(), eq(X_HIP_ID), any()))
                .thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/care-contexts/discover")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    public void shouldFireAndForgetForOnDiscover() throws JsonProcessingException {
        String requestId = UUID.randomUUID().toString();
        String callerRequestId = UUID.randomUUID().toString();
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("requestId", requestId);
        ObjectNode respNode = new ObjectMapper().createObjectNode();
        respNode.put("requestId", callerRequestId);
        objectNode.set("resp", respNode);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(objectNode));

        String testId = "testId";
        when(discoveryValidator.validateResponse(requestEntity, X_CM_ID))
                .thenReturn(Mono.just(new ValidatedResponse(testId, callerRequestId, objectNode)));
        when(validatedResponseAction.execute(eq(X_CM_ID), eq(testId), jsonNodeArgumentCaptor.capture()))
                .thenReturn(Mono.empty());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/care-contexts/on-discover")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }

}