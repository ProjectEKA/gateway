package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.common.model.ErrorResult;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static in.projecteka.gateway.common.Constants.TEMP_CM_ID;


public class HipConsentNotifyServiceClient extends ServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(HipConsentNotifyServiceClient.class);
    private CMRegistry cmRegistry;

    private static final String REQUEST_ROUTE = "/v1/consents/hip/notify";
    private static final String RESPONSE_ROUTE = "/v1/consents/hip/on-notify";

    public HipConsentNotifyServiceClient(ServiceOptions serviceOptions, WebClient.Builder webClientBuilder, CentralRegistry centralRegistry, CMRegistry cmRegistry) {
        super(serviceOptions, webClientBuilder, centralRegistry);
        this.cmRegistry = cmRegistry;
    }

    @Override
    public Mono<Void> routeRequest(Map<String, Object> request, String url) {
        return super.routeRequest(request, url + REQUEST_ROUTE);
    }

    @Override
    public Mono<Void> notifyError(ErrorResult request) {
        Optional<YamlRegistryMapping> config = cmRegistry.getConfigFor(TEMP_CM_ID);
        if (config.isEmpty()) {
            logger.error("No mapping found for " + TEMP_CM_ID);
            return Mono.error(ClientError.mappingNotFoundForId(TEMP_CM_ID));
        }
        return super.notifyError(request, config.get().getHost() + RESPONSE_ROUTE);
    }

    @Override
    public Mono<Void> routeResponse(JsonNode request, String cmUrl) {
        return Mono.empty();
    }
}
