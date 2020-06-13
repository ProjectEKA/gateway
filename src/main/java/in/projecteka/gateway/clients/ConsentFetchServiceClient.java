package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static in.projecteka.gateway.registry.ServiceType.HIU;

public class ConsentFetchServiceClient extends ServiceClient {
    private static final String REQUEST_ROUTE = "/v1/consents/fetch";
    private static final String RESPONSE_ROUTE = "/v1/consents/on-fetch";
    private final BridgeRegistry bridgeRegistry;
    private final CMRegistry cmRegistry;

    public ConsentFetchServiceClient(ServiceOptions serviceOptions,
                                     WebClient.Builder webClientBuilder,
                                     CentralRegistry centralRegistry,
                                     BridgeRegistry bridgeRegistry,
                                     CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.bridgeRegistry = bridgeRegistry;
        this.cmRegistry = cmRegistry;
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return bridgeRegistry.getConfigFor(clientId, HIU).map(host -> host + RESPONSE_ROUTE);
    }

    @Override
    protected Optional<String> getRequestUrl(String clientId) {
        return cmRegistry.getConfigFor(clientId).map(YamlRegistryMapping::getHost).map(host -> host + REQUEST_ROUTE);
    }
}
