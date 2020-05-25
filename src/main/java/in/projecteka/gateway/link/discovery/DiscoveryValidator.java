package in.projecteka.gateway.link.discovery;

import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.link.discovery.model.GatewayResponse;
import in.projecteka.gateway.link.discovery.model.PatientDiscoveryResult;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.gateway.link.discovery.Constants.REQUEST_ID;
import static in.projecteka.gateway.link.discovery.Constants.TEMP_CM_ID;
import static in.projecteka.gateway.link.discovery.Constants.TRANSACTION_ID;
import static in.projecteka.gateway.link.discovery.Constants.X_CM_ID;
import static in.projecteka.gateway.link.discovery.Constants.X_HIP_ID;

@AllArgsConstructor
public class DiscoveryValidator {
    BridgeRegistry bridgeRegistry;

    CMRegistry cmRegistry;

    DiscoveryServiceClient discoveryServiceClient;

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryValidator.class);

    Mono<ValidatedDiscoverRequest> validateDiscoverRequest(HttpEntity<String> requestEntity) {
        List<String> xHipIds = requestEntity.getHeaders().get(X_HIP_ID);
        if (xHipIds==null || xHipIds.isEmpty()) {
            return errorNotify(requestEntity, TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("X-HIP-ID missing on headers").build()).then(Mono.empty());
        }
        String xHipId = xHipIds.get(0);
        Optional<YamlRegistryMapping> hipConfig = bridgeRegistry.getConfigFor(xHipId, ServiceType.HIP);
        if (hipConfig.isEmpty()) {
            return errorNotify(requestEntity, TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("No mapping found for X-HIP-ID").build()).then(Mono.empty());
        }
        return Mono.just(new ValidatedDiscoverRequest(hipConfig.get()));
    }

    public Mono<Void> errorNotify(HttpEntity<String> requestEntity, String cmId, Error error) {
        return Utils.deserializeRequest(requestEntity)
                .map(deserializedRequest ->
                        Tuples.of((String)deserializedRequest.getOrDefault(REQUEST_ID,""),(String)deserializedRequest.getOrDefault(TRANSACTION_ID,"")))
                .filter(tuple -> {
                    String requestId = tuple.getT1();
                    String transactionId = tuple.getT2();
                    if (requestId.isEmpty()) {
                        logger.error("RequestId is empty");
                    }
                    if (transactionId.isEmpty()) {
                        logger.error("TransactionId is empty");
                    }
                    return !requestId.isEmpty() && !transactionId.isEmpty();
                })
                .map(tuple -> PatientDiscoveryResult.builder().error(error)
                        .resp(GatewayResponse.builder().requestId(UUID.fromString(tuple.getT1())).build())
                        .requestId(UUID.randomUUID())
                        .transactionId(UUID.fromString(tuple.getT2()))
                        .build()).flatMap(errorResult -> {
                    YamlRegistryMapping cmRegistryMapping = cmRegistry.getConfigFor(cmId).get();//TODO checkback when cmid is dynamic
                    return discoveryServiceClient.patientErrorResultNotify(errorResult,cmRegistryMapping.getHost());
                });
    }

    public Mono<ValidatedDiscoverResponse> validateDiscoverResponse(HttpEntity<String> requestEntity) {
        List<String> xCmIds = requestEntity.getHeaders().get(X_CM_ID);
        if (xCmIds==null || xCmIds.isEmpty()) {
            logger.error("No X-CM-ID found on Headers");
            return Mono.empty();
        }
        String xCmId = xCmIds.get(0);
        Optional<YamlRegistryMapping> cmConfig = cmRegistry.getConfigFor(xCmId);
        if (cmConfig.isEmpty()) {
            logger.error("No mapping found for X-CM-ID : {}",xCmId);
            return Mono.empty();
        }
        return Mono.just(new ValidatedDiscoverResponse(cmConfig.get()));
    }
}
