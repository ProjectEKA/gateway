package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public class DiscoveryServiceClient extends ServiceClient {
    private static final String REQUEST_ROUTE = "/v1/care-contexts/discover";
    private static final String RESPONSE_ROUTE = "/v1/care-contexts/on-discover";
    private final CMRegistry cmRegistry;

    public DiscoveryServiceClient(ServiceOptions serviceOptions,
                                  WebClient.Builder webClientBuilder,
                                  CMRegistry cmRegistry,
                                  CentralRegistry centralRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.cmRegistry = cmRegistry;
    }

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return super.routeRequest(request, url + REQUEST_ROUTE);
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return cmRegistry.getConfigFor(clientId).map(host -> host + RESPONSE_ROUTE);
    }
}
