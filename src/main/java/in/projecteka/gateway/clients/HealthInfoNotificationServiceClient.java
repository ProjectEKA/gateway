package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.CMRegistry;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

public class HealthInfoNotificationServiceClient extends ServiceClient{

    private final CMRegistry cmRegistry;
    private static final String REQUEST_ROUTE = "/v1/health-information/notify";

    public HealthInfoNotificationServiceClient(ServiceOptions serviceOptions,
                                               WebClient.Builder webClientBuilder,
                                               CentralRegistry centralRegistry,
                                               CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.cmRegistry = cmRegistry;
    }

    @Override
    protected Optional<String> getResponseUrl(String clientId) {
        return Optional.empty();
    }

    @Override
    protected Optional<String> getRequestUrl(String clientId) {
        return cmRegistry.getHostFor(clientId).map(host -> host + REQUEST_ROUTE);
    }}
