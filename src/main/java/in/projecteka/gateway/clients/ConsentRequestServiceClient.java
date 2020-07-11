package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static in.projecteka.gateway.registry.ServiceType.HIU;

public class ConsentRequestServiceClient extends ServiceClient {
    private final BridgeRegistry bridgeRegistry;
    private final CMRegistry cmRegistry;

    public ConsentRequestServiceClient(ServiceOptions serviceOptions,
                                       WebClient.Builder webClientBuilder,
                                       IdentityService identityService,
                                       BridgeRegistry bridgeRegistry,
                                       CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.bridgeRegistry = bridgeRegistry;
        this.cmRegistry = cmRegistry;
    }

    @Override
    protected Optional<String> getRequestUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + Constants.PATH_CONSENT_REQUESTS_INIT);
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return bridgeRegistry.getHostFor(clientId, HIU).map(host -> host + Constants.PATH_CONSENT_REQUESTS_ON_INIT);
    }
}
