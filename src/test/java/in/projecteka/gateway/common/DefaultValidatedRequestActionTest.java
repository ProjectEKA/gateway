package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.Map;

import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.Mockito.when;
import static reactor.test.StepVerifier.create;


class DefaultValidatedRequestActionTest {
    @Mock
    ServiceClient serviceClient;

    @InjectMocks
    DefaultValidatedRequestAction<ServiceClient> defaultValidatedRequestAction;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void invokeServiceClientForRoutingResponse() {
        String cmId = string();
        var mockRequest = (Map<String, Object>) Mockito.mock(Map.class);
        when(serviceClient.routeRequest(mockRequest, cmId)).thenReturn(Mono.empty());

        create(defaultValidatedRequestAction.routeRequest(cmId, mockRequest)).verifyComplete();
    }
}