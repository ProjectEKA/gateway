package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static in.projecteka.gateway.registry.ServiceType.HIU;

public class ConsentRequestServiceClient extends ServiceClient {
    private final BridgeRegistry bridgeRegistry;
    private static final String REQUEST_ROUTE = "/v1/consent-requests/init";
    private static final String RESPONSE_ROUTE = "/v1/consent-requests/on-init";

    public ConsentRequestServiceClient(ServiceOptions serviceOptions,
                                       WebClient.Builder webClientBuilder,
                                       BridgeRegistry bridgeRegistry,
                                       CentralRegistry centralRegistry) {
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
}
