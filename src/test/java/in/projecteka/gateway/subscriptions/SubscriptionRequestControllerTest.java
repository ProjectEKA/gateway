package in.projecteka.gateway.subscriptions;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.SubscriptionRequestNotifyServiceClient;
import in.projecteka.gateway.clients.SubscriptionRequestServiceClient;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
import in.projecteka.gateway.common.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static in.projecteka.gateway.common.Constants.X_HIU_ID;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HIU;
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
class SubscriptionRequestControllerTest {
    @MockBean
    RequestOrchestrator<SubscriptionRequestServiceClient> requestOrchestrator;

    @Qualifier("subscriptionResponseOrchestrator")
    @MockBean
    ResponseOrchestrator subscriptionResponseOrchestrator;

    @MockBean
    RequestOrchestrator<SubscriptionRequestNotifyServiceClient> notifyRequestOrchestrator;

    @Qualifier("subscriptionRequestNotifyResponseOrchestrator")
    @MockBean
    ResponseOrchestrator subscriptionNotifyResponseOrchestrator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    Validator subscriptionRequestValidator;

    @MockBean
    Authenticator authenticator;

    @Test
    void shouldFireAndForgetForSubscriptionRequestInit() {
        var token = string();
        var clientId = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIU)).build()));
        when(requestOrchestrator
                .handleThis(any(), eq(X_CM_ID), eq(X_HIU_ID), eq(BRIDGE_ID_PREFIX + clientId)))
                .thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_SUBSCRIPTION_REQUESTS_INIT_ON_GW)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldFireAndForgetForSubscriptionRequestOnInit() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(subscriptionResponseOrchestrator.processResponse(any(), eq(X_HIU_ID))).thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_SUBSCRIPTION_REQUESTS_ON_INIT_ON_GW)
                .header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldRouteNotifySubscriptionToHIU() {
        var token = string();
        var clientId = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(CM)).build()));
        when(notifyRequestOrchestrator
                .handleThis(any(), eq(X_HIU_ID), eq(X_CM_ID), eq(clientId)))
                .thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_SUBSCRIPTION_REQUESTS_NOTIFY)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldRouteOnNotifySubscriptionToHIU() {
        var token = string();
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(HIU)).build()));
        when(subscriptionNotifyResponseOrchestrator.processResponse(any(), eq(X_CM_ID))).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.PATH_SUBSCRIPTION_REQUESTS_ON_NOTIFY)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}