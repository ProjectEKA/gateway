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

import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.Mockito.when;
import static reactor.test.StepVerifier.create;


class DefaultValidatedResponseActionTest {
    @Mock
    ServiceClient serviceClient;

    @InjectMocks
    DefaultValidatedResponseAction<ServiceClient> defaultValidatedResponseAction;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void invokeServiceClientForRoutingResponse() {
        String cmId = string();
        JsonNode mockRequest = Mockito.mock(JsonNode.class);
        when(serviceClient.routeResponse(mockRequest, cmId)).thenReturn(Mono.empty());

        create(defaultValidatedResponseAction.routeResponse(cmId, mockRequest)).verifyComplete();
    }
}