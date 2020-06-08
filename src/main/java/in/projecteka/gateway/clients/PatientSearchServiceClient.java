package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static in.projecteka.gateway.registry.ServiceType.HIU;

public class PatientSearchServiceClient extends ServiceClient {
    private final BridgeRegistry bridgeRegistry;
    private static final String REQUEST_ROUTE = "/v1/patients/find";
    private static final String RESPONSE_ROUTE = "/v1/patients/on-find";

    public PatientSearchServiceClient(ServiceOptions serviceOptions,
                                      WebClient.Builder webClientBuilder,
                                      CentralRegistry centralRegistry,
                                      BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.bridgeRegistry = bridgeRegistry;
    }

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return super.routeRequest(request, url + REQUEST_ROUTE);
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return bridgeRegistry.getConfigFor(clientId, HIU).map(host -> host + RESPONSE_ROUTE);
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String url) {
        return super.routeResponse(request, url + RESPONSE_ROUTE);
    }
}
