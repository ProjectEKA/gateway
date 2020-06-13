package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static in.projecteka.gateway.registry.ServiceType.HIU;

public class HiuConsentNotifyServiceClient extends ServiceClient {
    private static final String REQUEST_ROUTE = "/v1/consents/hiu/notify";
    private static final String RESPONSE_ROUTE = "/v1/consents/hiu/on-notify";

    private final CMRegistry cmRegistry;
    private final BridgeRegistry bridgeRegistry;

    public HiuConsentNotifyServiceClient(ServiceOptions serviceOptions,
                                         WebClient.Builder webClientBuilder,
                                         CentralRegistry centralRegistry,
                                         CMRegistry cmRegistry,
                                         BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.cmRegistry = cmRegistry;
        this.bridgeRegistry = bridgeRegistry;
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String cmUrl) {
        return Mono.empty();
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return cmRegistry.getConfigFor(clientId).map(YamlRegistryMapping::getHost).map(host -> host + RESPONSE_ROUTE);
    }

    @Override
    protected Optional<String> getRequestUrl(String clientId) {
        return bridgeRegistry.getConfigFor(clientId, HIU).map(host -> host + REQUEST_ROUTE);
    }
}
