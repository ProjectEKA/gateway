package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ServiceClient;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static in.projecteka.gateway.common.Constants.X_CM_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class DefaultValidatedResponseActionTest {
    @Mock
    ServiceClient serviceClient;
    @Mock
    CMRegistry cmRegistry;
    @Mock
    YamlRegistryMapping cmConfig;
    @InjectMocks
    DefaultValidatedResponseAction<ServiceClient> defaultValidatedResponseAction;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldNotRouteResponseWhenCMIdIsNotPresent() {
        String cmId = "testCMId";
        when(cmRegistry.getConfigFor(cmId)).thenReturn(Optional.empty());

        StepVerifier.create(defaultValidatedResponseAction.routeResponse(X_CM_ID, cmId, null))
                .verifyComplete();
    }

    @Test
    void shouldRouteResponseWhenCmIdIsPresent() {
        String cmId = "testCMId";
        when(cmRegistry.getConfigFor(cmId)).thenReturn(Optional.of(cmConfig));
        String testHost = "testHost";
        when(cmConfig.getHost()).thenReturn(testHost);
        JsonNode mockRequest = Mockito.mock(JsonNode.class);
        when(serviceClient.routeResponse(mockRequest, testHost)).thenReturn(Mono.empty());

        StepVerifier.create(defaultValidatedResponseAction.routeResponse(X_CM_ID, cmId, mockRequest))
                .verifyComplete();
        verify(serviceClient).routeResponse(mockRequest, testHost);
    }

}