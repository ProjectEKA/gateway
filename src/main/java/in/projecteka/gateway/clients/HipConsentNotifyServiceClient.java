package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static in.projecteka.gateway.registry.ServiceType.HIP;

public class HipConsentNotifyServiceClient extends ServiceClient {
    private final CMRegistry cmRegistry;
    private final BridgeRegistry bridgeRegistry;

    public HipConsentNotifyServiceClient(ServiceOptions serviceOptions,
                                         WebClient.Builder webClientBuilder,
                                         IdentityService identityService,
                                         CMRegistry cmRegistry,
                                         BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.cmRegistry = cmRegistry;
        this.bridgeRegistry = bridgeRegistry;
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + Constants.PATH_CONSENTS_HIP_ON_NOTIFY);
    }

    @Override
    protected Optional<String> getRequestUrl(String clientId) {
        return bridgeRegistry.getHostFor(clientId, HIP).map(host -> host + Constants.PATH_CONSENTS_HIP_NOTIFY);
    }
}
