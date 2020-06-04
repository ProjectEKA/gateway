package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.common.model.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class ServiceClientTest {
    @Mock
    private ServiceOptions serviceOptions;

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
    void init(){
        MockitoAnnotations.initMocks(this);
        when(serviceOptions.getTimeout()).thenReturn(50);
        serviceClient = new ServiceClient(serviceOptions, webClientBuilder, centralRegistry) {
            @Override
            public Mono<Void> notifyError(ErrorResult errorResult) {
                return null;
            }
        };
    }

    @Test
    void shouldRouteGivenRequestToURL() {
        var token = "SAMPLE_TOKEN";
        var serializedRequest = "{}";
        var request = new HashMap<String,Object>();
        var url = "/temp-url";

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq(url))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(eq(MediaType.APPLICATION_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), eq(token))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(eq(serializedRequest));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        Mono<Void> emptyBodyMono = Mono.empty();
        when(responseSpec.bodyToMono(eq(Void.class))).thenReturn(emptyBodyMono);

        when(centralRegistry.authenticate()).thenReturn(Mono.just(token));

        StepVerifier.create(serviceClient.routeRequest(request,url))
                .verifyComplete();
    }

    @Test
    void shouldRouteGivenResponsetToURL() {
        var token = "SAMPLE_TOKEN";
        var serializedRequest = "{}";
        var mapper = new ObjectMapper();
        var request = mapper.createObjectNode();
        var url = "/temp-url";

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq(url))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(eq(MediaType.APPLICATION_JSON))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), eq(token))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(eq(serializedRequest));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        Mono<Void> emptyBodyMono = Mono.empty();
        when(responseSpec.bodyToMono(eq(Void.class))).thenReturn(emptyBodyMono);

        when(centralRegistry.authenticate()).thenReturn(Mono.just(token));

        StepVerifier.create(serviceClient.routeResponse(request,url))
                .verifyComplete();
    }

    @Test
    void shouldNotifyError() {

        var token = "SAMPLE_TOKEN";
        var url = "/temp-url";
        var uuid = UUID.randomUUID();
        var error = new Error(ErrorCode.INVALID_TOKEN,"invalid token");
        var link = new Object();
        var response = new GatewayResponse(uuid);
        var request = ErrorResult.builder().requestId(uuid).error(error).link(link).resp(response).build();

        when(centralRegistry.authenticate()).thenReturn(Mono.just(token));
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq(url))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), eq(token))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        Mono<Void> emptyBodyMono = Mono.empty();
        when(responseSpec.bodyToMono(eq(Void.class))).thenReturn(emptyBodyMono);

        StepVerifier.create(serviceClient.notifyError(request,url))
                .verifyComplete();

    }
}
