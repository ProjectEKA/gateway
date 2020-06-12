package in.projecteka.gateway.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Optional;

import static in.projecteka.gateway.testcommon.TestBuilders.errorResult;
import static in.projecteka.gateway.testcommon.TestBuilders.serviceOptions;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static in.projecteka.gateway.testcommon.TestEssentials.OBJECT_MAPPER;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static reactor.core.publisher.Mono.just;

@SuppressWarnings("ConstantConditions")
class ServiceClientTest {

    @Captor
    ArgumentCaptor<ClientRequest> captor;

    @Mock
    CentralRegistry centralRegistry;

    @Mock
    ExchangeFunction exchangeFunction;

    ServiceClient serviceClient;

    static final ServiceOptions SERVICE_OPTIONS = serviceOptions().timeout(1000).build();

    WebClient.Builder webClientBuilder;

    @BeforeEach
    void init() {
        initMocks(this);
        webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        serviceClient = new ServiceClient(serviceOptions().timeout(10000).build(), webClientBuilder, centralRegistry) {
            @Override
            protected Optional<String> getResponseUrl(String clientId) {
                return Optional.empty();
            }
        };
    }

    @Test
    void shouldRouteGivenRequestToURL() {
        var token = string();
        var request = new HashMap<String, Object>();
        var url = "/temp-url";
        when(centralRegistry.authenticate()).thenReturn(just(token));
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(just(ClientResponse.create(HttpStatus.OK)
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(serviceClient.routeRequest(request, url))
                .verifyComplete();
        assertThat(captor.getValue().url()).hasPath(url);
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(token);
    }

    @Test
    void shouldRouteGivenResponseToURL() {
        var token = string();
        var request = OBJECT_MAPPER.createObjectNode();
        var url = "/temp-url";
        when(centralRegistry.authenticate()).thenReturn(just(token));
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(just(ClientResponse.create(HttpStatus.OK)
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .build()));

        StepVerifier.create(serviceClient.routeResponse(request, url)).verifyComplete();

        assertThat(captor.getValue().url()).hasPath(url);
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(token);
    }

    @Test
    void shouldNotifyError() {
        var token = string();
        var url = "/temp-url";
        var clientId = string();
        var request = errorResult().build();
        when(centralRegistry.authenticate()).thenReturn(just(token));
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(just(ClientResponse.create(HttpStatus.OK)
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .build()));
        serviceClient = new ServiceClient(SERVICE_OPTIONS, webClientBuilder, centralRegistry) {
            @Override
            protected Optional<String> getResponseUrl(String clientId) {
                return of(url);
            }
        };

        StepVerifier.create(serviceClient.notifyError(clientId, request)).verifyComplete();
        assertThat(captor.getValue().url()).hasPath(url);
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(token);
    }

    @Test
    void logErrorReturnClientErrorWhenNot200SeriesResponseCame() throws JsonProcessingException {
        var token = string();
        var url = "/temp-url";
        var clientId = string();
        var request = errorResult().build();
        when(centralRegistry.authenticate()).thenReturn(just(token));
        var error = OBJECT_MAPPER.createObjectNode().put("error", "something went wrong");
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(just(ClientResponse.create(HttpStatus.GATEWAY_TIMEOUT)
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .body(OBJECT_MAPPER.writeValueAsString(error))
                        .build()));
        serviceClient = new ServiceClient(SERVICE_OPTIONS, webClientBuilder, centralRegistry) {
            @Override
            protected Optional<String> getResponseUrl(String clientId) {
                return of(url);
            }
        };

        StepVerifier.create(serviceClient.notifyError(clientId, request)).verifyError(ClientError.class);
        assertThat(captor.getValue().url()).hasPath(url);
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(token);
    }

    @Test
    void returnErrorIfUnableToFindAHostForAClient() {
        StepVerifier.create(serviceClient.notifyError(string(), errorResult().build())).verifyError(ClientError.class);
    }
}
