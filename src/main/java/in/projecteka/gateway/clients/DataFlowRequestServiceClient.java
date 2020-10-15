package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.registry.ServiceType.HIU;

public class DataFlowRequestServiceClient extends ServiceClient {

    private final BridgeRegistry bridgeRegistry;
    private final CMRegistry cmRegistry;

    public DataFlowRequestServiceClient(ServiceOptions serviceOptions,
                                        WebClient.Builder webClientBuilder,
                                        IdentityService identityService,
                                        BridgeRegistry bridgeRegistry,
                                        CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.bridgeRegistry = bridgeRegistry;
        this.cmRegistry = cmRegistry;
    }

    @Override
    protected Mono<String> getResponseUrl(String clientId, ServiceType serviceType) {
        return bridgeRegistry.getHostFor(clientId, HIU).map(host -> host + Constants.CALLBACK_PATH_HIU_HEALTH_INFORMATION_REQUEST);
    }

    @Override
    protected Mono<String> getRequestUrl(String clientId, ServiceType serviceType) {
        return cmRegistry.getHostFor(clientId).map(host -> host + Constants.ROUTE_PATH_CM_HEALTH_INFORMATION_REQUEST);
    }
}
