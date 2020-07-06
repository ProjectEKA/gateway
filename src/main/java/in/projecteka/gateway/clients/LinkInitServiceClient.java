package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.registry.ServiceType.HIP;

public class LinkInitServiceClient extends ServiceClient {
    private static final String REQUEST_ROUTE = "/v1/links/link/init";
    private static final String RESPONSE_ROUTE = "/v1/links/link/on-init";
    private final CMRegistry cmRegistry;
    private final BridgeRegistry bridgeRegistry;

    public LinkInitServiceClient(WebClient.Builder webClientBuilder,
                                 ServiceOptions serviceOptions,
                                 IdentityService identityService,
                                 CMRegistry cmRegistry,
                                 BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.cmRegistry = cmRegistry;
        this.bridgeRegistry = bridgeRegistry;
    }

    @Override
    protected Mono<String> getResponseUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + RESPONSE_ROUTE);
    }

    @Override
    protected Mono<String> getRequestUrl(String clientId) {
        return bridgeRegistry.getHostFor(clientId, HIP).map(host -> host + REQUEST_ROUTE);
    }
}
