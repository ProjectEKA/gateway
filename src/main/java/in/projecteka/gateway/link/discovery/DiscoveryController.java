package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.clients.DiscoveryServiceClient;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class DiscoveryController {
    ObjectMapper objectMapper = new ObjectMapper();//TODO
    Map<String,String> cacheMap = new HashMap<>();//TODO
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
        UUID gatewayRequestId = UUID.randomUUID();
        return deserializeRequest(requestEntity).map(deserializedRequest -> {
            String cmRequestId = (String) deserializedRequest.get("requestId");
            cacheMap.put(gatewayRequestId.toString(), cmRequestId);
            deserializedRequest.put("requestId", gatewayRequestId);
            return deserializedRequest;
        }).flatMap(updatedRequest -> discoveryServiceClient
                .patientFor(updatedRequest, hipConfig.get().getHost())
                .onErrorResume(throwable -> Mono.empty()));//TODO call on complete
    }

    private Mono<Map<String, Object>> deserializeRequest(HttpEntity<String> requestEntity) {
        try {
            return Mono.just(objectMapper.readValue(requestEntity.getBody(), new TypeReference<>() { }));
        } catch (JsonProcessingException e) {
            logger.error("Error in deserializing", e);
            return Mono.empty();
        }
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
        return deserializeRequest(requestEntity).map(deserializedRequest -> {
            String gatewayRequestId = (String) deserializedRequest.get("requestId");
            String cmRequestId = cacheMap.get(gatewayRequestId);
            deserializedRequest.put("requestId",cmRequestId);
            return deserializedRequest;
        }).flatMap(updatedRequest -> discoveryServiceClient
                .patientDiscoveryResultNotify(updatedRequest,cmConfig.get().getHost()));
    }
}
