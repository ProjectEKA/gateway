package in.projecteka.gateway.link.discovery;

import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RestController
public class DiscoveryController {
    public static final String X_HIP_ID = "X-HIP-ID";
    @Autowired
    BridgeRegistry bridgeRegistry;
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
}
