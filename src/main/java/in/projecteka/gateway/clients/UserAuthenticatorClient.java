package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class UserAuthenticatorClient extends ServiceClient{

    private final CMRegistry cmRegistry;
    private final BridgeRegistry bridgeRegistry;

    public UserAuthenticatorClient(ServiceOptions serviceOptions,
                                  WebClient.Builder webClientBuilder,
                                  IdentityService identityService,
                                  CMRegistry cmRegistry,
                                  BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.cmRegistry = cmRegistry;
        this.bridgeRegistry = bridgeRegistry;
    }

    private Mono<String> getHIUHost(String clientId){
        return bridgeRegistry.getHostFor(clientId, ServiceType.HIU)
                .map(host -> host);
    }

    @Override
    protected Mono<String> getResponseUrl(String clientId) {
        return bridgeRegistry.getHostFor(clientId, ServiceType.HIP)
                .switchIfEmpty(this.getHIUHost(clientId))
                .map(host -> host + Constants.PATH_USERS_AUTH_ON_INIT);
    }

    @Override
    protected Mono<String> getRequestUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + Constants.PATH_USERS_AUTH_INIT);
    }
}
