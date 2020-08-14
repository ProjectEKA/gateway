package in.projecteka.gateway.link.link;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.HipInitLinkServiceClient;
import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
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

import static in.projecteka.gateway.common.Constants.BRIDGE_ID_PREFIX;
import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static in.projecteka.gateway.common.Constants.X_HIP_ID;
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
public class LinkControllerTest {
    @MockBean
    RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator;

    @Qualifier("linkInitResponseOrchestrator")
    @MockBean
    ResponseOrchestrator linkInitResponseOrchestrator;

    @MockBean
    RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator;

    @MockBean
    RequestOrchestrator<HipInitLinkServiceClient> hipInitLinkRequestOrchestrator;

    @Qualifier("hipInitLinkResponseOrchestrator")
    @MockBean
    ResponseOrchestrator hipInitLinkResponseOrchestrator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @MockBean
    Authenticator authenticator;

    @Test
    void shouldFireAndForgetForLinkInit() {
        var token = string();
        var clientId = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(CM)).build()));
        when(linkInitRequestOrchestrator.handleThis(any(), eq(X_HIP_ID), eq(X_CM_ID), eq(clientId))).thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_LINK_INIT)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    public void shouldFireAndForgetForLinkOnInit() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(HIP)).build()));
        when(linkInitResponseOrchestrator.processResponse(any(), eq(X_CM_ID)))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.PATH_LINK_ON_INIT)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForLinkConfirm() {
        var clientId = string();
        var token = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(CM)).build()));
        when(linkConfirmRequestOrchestrator.handleThis(any(), eq(X_HIP_ID), eq(X_CM_ID), eq(clientId))).thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_LINK_CONFIRM)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForHipInitLink() {
        var token = string();
        var clientId = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIP)).build()));
        when(hipInitLinkRequestOrchestrator
                .handleThis(any(), eq(X_CM_ID), eq(X_HIP_ID), eq(BRIDGE_ID_PREFIX + clientId)))
                .thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_ADD_CARE_CONTEXTS)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    public void shouldFireAndForgetForHipInitOnLink() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(hipInitLinkResponseOrchestrator.processResponse(any(), eq(X_HIP_ID)))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.PATH_ON_ADD_CARE_CONTEXTS)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

}
