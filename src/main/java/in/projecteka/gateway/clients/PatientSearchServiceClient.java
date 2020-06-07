package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static in.projecteka.gateway.common.Constants.TEMP_HIU_ID;

public class PatientSearchServiceClient extends ServiceClient{
    private static final Logger logger = LoggerFactory.getLogger(ConsentFetchServiceClient.class);
    private final BridgeRegistry bridgeRegistry;
    private static final String REQUEST_ROUTE = "/v1/patients/find";
    private static final String RESPONSE_ROUTE = "/v1/patients/on-find";

    public PatientSearchServiceClient(ServiceOptions serviceOptions, WebClient.Builder webClientBuilder, CentralRegistry centralRegistry,BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.bridgeRegistry = bridgeRegistry;
    }

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return super.routeRequest(request, url + REQUEST_ROUTE);
    }


    @Override
    public Mono<Void> notifyError(ErrorResult request) {
        Optional<YamlRegistryMapping> config = bridgeRegistry.getConfigFor(TEMP_HIU_ID, ServiceType.HIU);
        if (config.isEmpty()) {
            logger.error("No mapping found for " + TEMP_HIU_ID);
            return Mono.error(ClientError.mappingNotFoundForId(TEMP_HIU_ID));
        }

        return super.notifyError(request, config.get().getHost() + RESPONSE_ROUTE);
    }
}
