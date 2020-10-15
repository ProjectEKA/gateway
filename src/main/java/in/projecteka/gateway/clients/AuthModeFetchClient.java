package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class AuthModeFetchClient extends ServiceClient {
    private final BridgeRegistry bridgeRegistry;
    private final CMRegistry cmRegistry;

    public AuthModeFetchClient(ServiceOptions serviceOptions,
                               WebClient.Builder webClientBuilder,
                               IdentityService identityService,
                               BridgeRegistry bridgeRegistry,
                               CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.bridgeRegistry = bridgeRegistry;
        this.cmRegistry = cmRegistry;
    }

    private Mono<String> getHIUHost(String clientId){
        return bridgeRegistry.getHostFor(clientId, ServiceType.HIU)
                .map(host -> host + Constants.PATH_BRIDGE_ON_FETCH_AUTH_MODES);
    }

    @Override
    protected Mono<String> getResponseUrl(String clientId, ServiceType serviceType) {
        return bridgeRegistry.getHostFor(clientId, ServiceType.HIP)
                .switchIfEmpty(this.getHIUHost(clientId))
                .map(host -> host + Constants.PATH_BRIDGE_ON_FETCH_AUTH_MODES);
    }

    @Override
    protected Mono<String> getRequestUrl(String clientId, ServiceType serviceType) {
        return cmRegistry.getHostFor(clientId).map(host -> host + Constants.PATH_FETCH_AUTH_MODES);
    }
}
