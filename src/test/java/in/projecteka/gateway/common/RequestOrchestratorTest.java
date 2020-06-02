package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.ClientError;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.common.model.ErrorResult;
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

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestOrchestratorTest {
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
    RequestOrchestrator requestOrchestrator;
    @Mock
    CMRegistry cmRegistry;
    @Captor
    ArgumentCaptor<ErrorResult> discoveryResultArgumentCaptor;
    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        requestOrchestrator = Mockito.spy(new RequestOrchestrator(requestIdMappings,discoveryValidator,discoveryServiceClient,cmRegistry));
    }

    @Test
    public void shouldNotCallBridgeWhenRequestIdIsNotPresent() {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID)).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyComplete();
    }

    @Test
    public void shouldNotifyErrorWhenValidationFails() throws JsonProcessingException {
        HttpEntity<String> requestEntity = new HttpEntity<>("");
        ClientError clientError = ClientError.idMissingInHeader(X_CM_ID);
        doReturn(Mono.empty()).when(requestOrchestrator).errorNotify(requestEntity, clientError.getError().getError());
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID)).thenReturn(Mono.error(clientError));

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyComplete();
        verify(requestOrchestrator).errorNotify(requestEntity, clientError.getError().getError());
    }

    @Test
    public void shouldCallBridgeWhenValidRequest() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID)).thenReturn(Mono.just(new ValidatedRequest(hipConfig,requestId,requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(),eq(testhost))).thenReturn(Mono.empty());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
    }

    @Test
    public void shouldNotifyCMOnBridgeTimeout() throws JsonProcessingException {
        Map<String,Object> requestBody = new HashMap<>();
        String requestId = UUID.randomUUID().toString();
        requestBody.put("requestId", requestId);
        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(requestBody));

        String testhost = "testhost";
        when(hipConfig.getHost()).thenReturn(testhost);
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID)).thenReturn(Mono.just(new ValidatedRequest(hipConfig,requestId,requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(),eq(testhost))).thenReturn(Mono.error(new TimeoutException()));
        doReturn(Mono.empty()).when(requestOrchestrator).errorNotify(eq(requestEntity), errorArgumentCaptor.capture());

        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
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
        when(discoveryValidator.validateRequest(requestEntity, X_CM_ID)).thenReturn(Mono.just(new ValidatedRequest(hipConfig,requestId,requestBody)));
        when(requestIdMappings.put(requestIdCaptor.capture(), eq(requestId))).thenReturn(Mono.empty());
        when(discoveryServiceClient.routeRequest(captor.capture(),eq(testhost))).thenReturn(Mono.error(new RuntimeException()));

        doReturn(Mono.empty()).when(requestOrchestrator).errorNotify(eq(requestEntity), errorArgumentCaptor.capture());
        StepVerifier.create(requestOrchestrator.processRequest(requestEntity, X_CM_ID))
                .verifyComplete();

        verify(hipConfig).getHost();
        verify(discoveryValidator).validateRequest(requestEntity, X_CM_ID);
        Assertions.assertEquals(requestIdCaptor.getValue(),((UUID)captor.getValue().get("requestId")).toString());
        Assertions.assertEquals("Error in making call to Bridge",errorArgumentCaptor.getValue().getMessage());
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
        when(discoveryServiceClient.notifyError(discoveryResultArgumentCaptor.capture())).thenReturn(Mono.empty());

        Error error = new Error();
        StepVerifier.create(requestOrchestrator.errorNotify(requestEntity, error))
                .verifyComplete();

        Assertions.assertEquals(error,discoveryResultArgumentCaptor.getValue().getError());

    }
}