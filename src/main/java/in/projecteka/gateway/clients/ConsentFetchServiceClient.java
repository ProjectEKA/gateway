package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.registry.ServiceType.HIU;

public class ConsentFetchServiceClient extends ServiceClient {
    private static final String REQUEST_ROUTE = "/v1/consents/fetch";
    private static final String RESPONSE_ROUTE = "/v1/consents/on-fetch";
    private final BridgeRegistry bridgeRegistry;
    private final CMRegistry cmRegistry;

    public ConsentFetchServiceClient(ServiceOptions serviceOptions,
                                     WebClient.Builder webClientBuilder,
                                     IdentityService identityService,
                                     BridgeRegistry bridgeRegistry,
                                     CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.bridgeRegistry = bridgeRegistry;
        this.cmRegistry = cmRegistry;
    }

    @Override
    protected Mono<String> getResponseUrl(String clientId) {
        return bridgeRegistry.getHostFor(clientId, HIU).map(host -> host + RESPONSE_ROUTE);
    }

    @Override
    protected Mono<String> getRequestUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + REQUEST_ROUTE);
    }
}
