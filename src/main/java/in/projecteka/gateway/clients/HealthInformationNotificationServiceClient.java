package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public class HealthInformationNotificationServiceClient extends ServiceClient{

    private final BridgeRegistry bridgeRegistry;
    private final CMRegistry cmRegistry;
    private static final String REQUEST_ROUTE = "/v1/health-information/notify";

    public HealthInformationNotificationServiceClient(ServiceOptions serviceOptions,
                                                      WebClient.Builder webClientBuilder,
                                                      CentralRegistry centralRegistry,
                                                      BridgeRegistry bridgeRegistry,
                                                      CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.bridgeRegistry = bridgeRegistry;
        this.cmRegistry = cmRegistry;
    }

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return super.routeRequest(request, url + REQUEST_ROUTE);
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return Optional.empty();
    }

    @Override
    protected Optional<String> getRequestUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + REQUEST_ROUTE);
    }}
