package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static in.projecteka.gateway.registry.ServiceType.HIP;

public class HipConsentNotifyServiceClient extends ServiceClient {
    private static final String REQUEST_ROUTE = "/v1/consents/hip/notify";
    private static final String RESPONSE_ROUTE = "/v1/consents/hip/on-notify";

    private final CMRegistry cmRegistry;
    private final BridgeRegistry bridgeRegistry;

    public HipConsentNotifyServiceClient(ServiceOptions serviceOptions,
                                         WebClient.Builder webClientBuilder,
                                         CentralRegistry centralRegistry,
                                         CMRegistry cmRegistry,
                                         BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.cmRegistry = cmRegistry;
        this.bridgeRegistry = bridgeRegistry;
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + RESPONSE_ROUTE);
    }

    @Override
    protected Optional<String> getRequestUrl(String clientId) {
        return bridgeRegistry.getHostFor(clientId, HIP).map(host -> host + REQUEST_ROUTE);
    }
}
