package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class DiscoveryController {
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryController.class);
    public static final String X_HIP_ID = "X-HIP-ID";
    public static final String X_CM_ID = "X-CM-ID";
    @Autowired
    BridgeRegistry bridgeRegistry;
    @Autowired
    CMRegistry cmRegistry;
    @Autowired
    DiscoveryServiceClient discoveryServiceClient;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/patients/care-contexts/discover")
    public Mono<Void> discoverCareContext(HttpEntity<String> requestEntity) {
        List<String> xHipIds = requestEntity.getHeaders().get(X_HIP_ID);
        Mono<Void> tobeFiredAndForgotten = doDiscoverCareContext(requestEntity, xHipIds);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    private Mono<Void> doDiscoverCareContext(HttpEntity<String> requestEntity, List<String> xHipIds) {
        if (xHipIds==null || xHipIds.isEmpty()) {
            //TODO error handling
            return Mono.empty();
        }
        String xHipId = xHipIds.get(0);
        Optional<YamlRegistryMapping> hipConfig = bridgeRegistry.getConfigFor(xHipId, ServiceType.HIP);
        if (hipConfig.isEmpty()) {
           //TODO error handling
            return Mono.empty();
        }
        return discoveryServiceClient.patientFor(requestEntity.getBody(), hipConfig.get().getHost())
                .onErrorResume(throwable -> Mono.empty());//TODO call on complete
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/patients/care-contexts/on-discover")
    public Mono<Void> onDiscoverCareContext(HttpEntity<String> requestEntity) {
        List<String> xCmIds = requestEntity.getHeaders().get(X_CM_ID);
        Mono<Void> tobeFiredAndForgotten = doOnDiscoverCareContext(requestEntity, xCmIds);
        tobeFiredAndForgotten.subscribe();
        return Mono.empty();
    }

    private Mono<Void> doOnDiscoverCareContext(HttpEntity<String> requestEntity, List<String> xCmIds) {
        String xCmId = xCmIds.get(0);
        Optional<YamlRegistryMapping> cmConfig = cmRegistry.getConfigFor(xCmId);
        return discoveryServiceClient.patientDiscoveryResultNotify(requestEntity.getBody(),cmConfig.get().getHost());
    }
}
