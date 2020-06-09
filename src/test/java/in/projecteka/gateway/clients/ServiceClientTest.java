package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Optional;

import static in.projecteka.gateway.testcommon.TestBuilders.errorResult;
import static in.projecteka.gateway.testcommon.TestBuilders.serviceOptions;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

class ServiceClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private CentralRegistry centralRegistry;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<WebClient.RequestBodySpec> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ServiceClient serviceClient;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        when(webClientBuilder.build()).thenReturn(webClient);
        serviceClient = new ServiceClient(serviceOptions().build(), webClientBuilder, centralRegistry) {
            @Override
            protected Optional<String> getResponseUrl(String clientId) {
                return Optional.empty();
            }
        };
    }

    @Test
    void shouldRouteGivenRequestToURL() {
        var token = string();
        var serializedRequest = "{}";
        var request = new HashMap<String, Object>();
        var url = "/temp-url";
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq(url))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(eq(MediaType.APPLICATION_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), eq(token))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(eq(serializedRequest));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(empty());

        when(centralRegistry.authenticate()).thenReturn(just(token));

        StepVerifier.create(serviceClient.routeRequest(request, url)).verifyComplete();
        reset(webClient);
        reset(requestBodyUriSpec);
        reset(requestBodySpec);
        reset(requestHeadersSpec);
        reset(responseSpec);
    }

    @Test
    void shouldRouteGivenResponseToURL() {
        var token = string();
        var serializedRequest = "{}";
        var request = OBJECT_MAPPER.createObjectNode();
        var url = "/temp-url";
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq(url))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(eq(MediaType.APPLICATION_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), eq(token))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(eq(serializedRequest));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(empty());

        when(centralRegistry.authenticate()).thenReturn(just(token));

        StepVerifier.create(serviceClient.routeResponse(request, url)).verifyComplete();
        reset(webClient);
        reset(requestBodyUriSpec);
        reset(requestBodySpec);
        reset(requestHeadersSpec);
        reset(responseSpec);
    }

    @Test
    void shouldNotifyError() {
        var token = string();
        var url = "/temp-url";
        var clientId = string();
        var request = errorResult().build();
        when(centralRegistry.authenticate()).thenReturn(just(token));
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(eq(MediaType.APPLICATION_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), eq(token))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(empty());
        serviceClient = new ServiceClient(serviceOptions().build(), webClientBuilder, centralRegistry) {
            @Override
            protected Optional<String> getResponseUrl(String clientId) {
                return of(url);
            }
        };

        StepVerifier.create(serviceClient.notifyError(clientId, request)).verifyComplete();
        reset(webClient);
        reset(requestBodyUriSpec);
        reset(requestBodySpec);
        reset(requestHeadersSpec);
        reset(responseSpec);
    }

    @Test
    void returnErrorIfUnableToFindAHostForAClient() {
        StepVerifier.create(serviceClient.notifyError(string(), errorResult().build())).verifyError(ClientError.class);
    }
}
