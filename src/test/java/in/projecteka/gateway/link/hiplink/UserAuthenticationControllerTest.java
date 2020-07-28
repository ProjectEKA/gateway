package in.projecteka.gateway.link.hiplink;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.UserAuthenticatorClient;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.RequestOrchestrator;
import in.projecteka.gateway.common.ResponseOrchestrator;
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
public class UserAuthenticationControllerTest {
    @MockBean
    RequestOrchestrator<UserAuthenticatorClient> userAuthenticationRequestOrchestrator;

    @Qualifier("userAuthenticationResponseOrchestrator")
    @MockBean
    ResponseOrchestrator userAuthenticationResponseOrchestrator;

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    Authenticator authenticator;

    @Test
    void shouldFireAndForgetForUsersAuthInit() {
        var token = string();
        var clientId = string();
        when(authenticator.verify(token))
                .thenReturn(just(caller().clientId(clientId).roles(List.of(HIP)).build()));
        when(userAuthenticationRequestOrchestrator.handleThis(any(), eq(X_CM_ID), eq(X_HIP_ID), eq(clientId))).thenReturn(empty());

        webTestClient
                .post()
                .uri(Constants.PATH_USERS_AUTH_INIT)
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
        when(authenticator.verify(token)).thenReturn(just(caller().roles(List.of(CM)).build()));
        when(userAuthenticationResponseOrchestrator.processResponse(any(), eq(X_HIP_ID)))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.PATH_USERS_AUTH_ON_INIT)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}
