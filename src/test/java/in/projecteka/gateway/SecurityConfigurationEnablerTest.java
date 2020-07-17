package in.projecteka.gateway;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.common.AdminAuthenticator;
import in.projecteka.gateway.common.Caller;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.Role;
import in.projecteka.gateway.registry.RegistryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecurityConfigurationEnablerTest {

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean
    Authenticator authenticator;

    @MockBean
    AdminAuthenticator adminAuthenticator;

    @MockBean
    RegistryService registryService;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void return401UnAuthorized() {
        webTestClient
                .post()
                .uri(Constants.PATH_PATIENTS_FIND)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isEqualTo(UNAUTHORIZED);
    }

    @Test
    void return403Forbidden() {
        var token = string();
        var caller = Caller.builder().roles(List.of(Role.CM)).build();
        when(authenticator.verify(token)).thenReturn(Mono.just(caller));

        webTestClient
                .post()
                .uri(Constants.PATH_PATIENTS_FIND)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void return403ForbiddenError() {
        var token = string();
        var caller = Caller.builder().roles(List.of(Role.CM, Role.GATEWAY, Role.HIP, Role.HIU)).build();
        when(adminAuthenticator.verify(token)).thenReturn(Mono.just(caller));

        webTestClient
                .post()
                .uri(Constants.INTERNAL_BRIDGES)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void return5xxSeverError() {
        var token = string();
        var caller = Caller.builder().roles(List.of(Role.ADMIN)).build();
        when(adminAuthenticator.verify(token)).thenReturn(Mono.just(caller));
        when(registryService.populateBridgeEntry(any())).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.INTERNAL_BRIDGES)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isOk();
    }
}
