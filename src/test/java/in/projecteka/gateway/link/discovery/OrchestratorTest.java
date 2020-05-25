package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.link.common.ValidatedRequest;
import in.projecteka.gateway.link.common.ValidatedResponse;
import in.projecteka.gateway.link.common.Validator;
import in.projecteka.gateway.link.common.model.ErrorResult;
import in.projecteka.gateway.registry.CMRegistry;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static in.projecteka.gateway.link.common.Constants.TEMP_CM_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrchestratorTest {
    @Mock
    Validator discoveryValidator;
    @Mock
    YamlRegistryMapping hipConfig;
    @Mock
    YamlRegistryMapping cmConfig;
    @Mock
    CacheAdapter<String,String> requestIdMappings;
    @Mock
    DiscoveryServiceClient discoveryServiceClient;
    private @Captor ArgumentCaptor<Map<String,Object>> captor;
    private @Captor ArgumentCaptor<String> requestIdCaptor;
    private @Captor ArgumentCaptor<Error> errorArgumentCaptor;
    private @Captor ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;
    Orchestrator orchestrator;
    @Mock
    CMRegistry cmRegistry;
    @Captor
    ArgumentCaptor<ErrorResult> discoveryResultArgumentCaptor;
    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        orchestrator = Mockito.spy(new Orchestrator(requestIdMappings,discoveryValidator,discoveryServiceClient,cmRegistry));
    }

    @Test
    public void shouldNotCallBridgeWhenRequestIdIsNotPresent() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        when(discoveryValidator.validateRequest(requestEntity)).thenReturn(Mono.empty());

        StepVerifier.create(orchestrator.processRequest(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldNotCallCMonValidationErrors() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        when(discoveryValidator.validateResponse(requestEntity)).thenReturn(Mono.empty());

        StepVerifier.create(orchestrator.processResponse(requestEntity))
                .verifyComplete();
    }

    @Test
    public void shouldNotifyErrorWhenValidationFails() throws JsonProcessingException {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        ClientError clientError = ClientError.hipIdMissing();
        doReturn(Mono.empty()).when(orchestrator).errorNotify(requestEntity,TEMP_CM_ID,clientError.getError().getError());
        when(discoveryValidator.validateRequest(requestEntity)).thenReturn(Mono.error(clientError));

        StepVerifier.create(orchestrator.processRequest(requestEntity))
                .verifyComplete();
        verify(orchestrator).errorNotify(requestEntity,TEMP_CM_ID,clientError.getError().getError());
    }

    @Test
    public void shouldCallBridgeWhenValidRequest() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateRequest(requestEntity)).thenReturn(Mono.just(new ValidatedRequest(hipConfig,requestId,requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(),eq(testhost))).thenReturn(Mono.empty());

        StepVerifier.create(orchestrator.processRequest(requestEntity))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateRequest(requestEntity);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
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

        StepVerifier.create(orchestrator.processResponse(requestEntity))
                .verifyComplete();

        verify(cmConfig).getHost();
        verify(discoveryValidator).validateResponse(requestEntity);
        Assertions.assertEquals(cmRequestId,jsonNodeArgumentCaptor.getValue().path("resp").path("requestId").asText());
    }


    @Test
    public void shouldNotifyCMOnBridgeTimeout() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateRequest(requestEntity)).thenReturn(Mono.just(new ValidatedRequest(hipConfig,requestId,requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(),eq(testhost))).thenReturn(Mono.error(new TimeoutException()));
        doReturn(Mono.empty()).when(orchestrator).errorNotify(eq(requestEntity),eq(TEMP_CM_ID),errorArgumentCaptor.capture());

        StepVerifier.create(orchestrator.processRequest(requestEntity))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateRequest(requestEntity);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
        Assertions.assertEquals("Timedout When calling bridge",errorArgumentCaptor.getValue().getMessage());
    }

    @Test
    public void shouldNotifyCMOnBridgeError() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateRequest(requestEntity)).thenReturn(Mono.just(new ValidatedRequest(hipConfig,requestId,requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(),eq(testhost))).thenReturn(Mono.error(new RuntimeException()));

        doReturn(Mono.empty()).when(orchestrator).errorNotify(eq(requestEntity),eq(TEMP_CM_ID),errorArgumentCaptor.capture());
        StepVerifier.create(orchestrator.processRequest(requestEntity))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateRequest(requestEntity);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
        Assertions.assertEquals("Error in making call to Bridge",errorArgumentCaptor.getValue().getMessage());
    }



    @Test
    public void shouldNotErrorNotifyWhenTransactionIdIsNotPresent() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("requestId", "testRequestId");
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        StepVerifier.create(orchestrator.errorNotify(requestEntity,null,null))
                .verifyComplete();
    }

    @Test
    public void shouldNotifyError() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("requestId", UUID.randomUUID());
        requestBody.put("transactionId", UUID.randomUUID());
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));
        String cmId = "testCmId";
        when(cmRegistry.getConfigFor(cmId)).thenReturn(Optional.of(cmConfig));
        String testCmHost = "testCmHost";
        when(cmConfig.getHost()).thenReturn(testCmHost);
        when(discoveryServiceClient.notifyError(discoveryResultArgumentCaptor.capture(),eq(testCmHost))).thenReturn(Mono.empty());

        Error error = new Error();
        StepVerifier.create(orchestrator.errorNotify(requestEntity,cmId,error))
                .verifyComplete();

        Assertions.assertEquals(error,discoveryResultArgumentCaptor.getValue().getError());

    }
}