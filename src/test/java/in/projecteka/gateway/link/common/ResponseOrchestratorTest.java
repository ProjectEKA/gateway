package in.projecteka.gateway.link.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResponseOrchestratorTest {
    @Mock
    Validator discoveryValidator;
    @Mock
    YamlRegistryMapping cmConfig;
    @Mock
    CacheAdapter<String,String> requestIdMappings;
    @Mock
    DiscoveryServiceClient discoveryServiceClient;
    private @Captor ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;
    ResponseOrchestrator responseOrchestrator;
    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        responseOrchestrator = Mockito.spy(new ResponseOrchestrator(discoveryValidator,discoveryServiceClient));
    }
    @Test
    public void shouldNotCallCMonValidationErrors() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        when(discoveryValidator.validateResponse(requestEntity)).thenReturn(Mono.empty());

        StepVerifier.create(responseOrchestrator.processResponse(requestEntity))
                .verifyComplete();
    }
    @Test
    public void shouldCallCMWhenValidRequest() throws JsonProcessingException {
        String requestId = UUID.randomUUID().toString();
        String cmRequestId = UUID.randomUUID().toString();
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("requestId",requestId);
        ObjectNode respNode = new ObjectMapper().createObjectNode();
        respNode.put("requestId",cmRequestId);
        objectNode.set("resp",respNode);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(objectNode));

        String testhost = "testhost";
        when(cmConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateResponse(requestEntity)).thenReturn(Mono.just(new ValidatedResponse(cmConfig,cmRequestId, objectNode)));
        when(requestIdMappings.get(eq(requestId))).thenReturn(Mono.just(cmRequestId));
        when(discoveryServiceClient.routeResponse(jsonNodeArgumentCaptor.capture(),eq(testhost))).thenReturn(Mono.empty());

        StepVerifier.create(responseOrchestrator.processResponse(requestEntity))
                .verifyComplete();

        verify(cmConfig).getHost();
        verify(discoveryValidator).validateResponse(requestEntity);
        Assertions.assertEquals(cmRequestId,jsonNodeArgumentCaptor.getValue().path("resp").path("requestId").asText());
    }

}