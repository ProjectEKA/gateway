package in.projecteka.gateway.link.link;

import in.projecteka.gateway.clients.LinkConfirmServiceClient;
import in.projecteka.gateway.clients.LinkInitServiceClient;
import in.projecteka.gateway.link.common.RequestOrchestrator;
import in.projecteka.gateway.link.common.ResponseOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LinkControllerTest {
    @MockBean
    RequestOrchestrator<LinkInitServiceClient> linkInitRequestOrchestrator;

    @Qualifier("linkInitResponseOrchestrator")
    @MockBean
    ResponseOrchestrator linkInitResponseOrchestrator;
    @MockBean
    RequestOrchestrator<LinkConfirmServiceClient> linkConfirmRequestOrchestrator;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void shouldFireAndForgetForLinkInit() {
        Mockito.when(linkInitRequestOrchestrator.processRequest(Mockito.any())).thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/links/link/init")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    public void shouldFireAndForgetForLinkOnInit() {
        Mockito.when(linkInitResponseOrchestrator.processResponse(Mockito.any())).thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/links/link/on-init")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    public void shouldFireAndForgetForLinkConfirm() {
        Mockito.when(linkConfirmRequestOrchestrator.processRequest(Mockito.any())).thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

        WebTestClient mutatedWebTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build();
        mutatedWebTestClient
                .post()
                .uri("/v1/links/link/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();
    }
}
