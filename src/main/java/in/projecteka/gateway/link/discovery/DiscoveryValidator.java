package in.projecteka.gateway.link.discovery;

import in.projecteka.gateway.clients.DiscoveryServiceClient;
import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.link.discovery.model.PatientDiscoveryResult;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import in.projecteka.gateway.registry.YamlRegistryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static in.projecteka.gateway.link.discovery.Constants.REQUEST_ID;
import static in.projecteka.gateway.link.discovery.Constants.TEMP_CM_ID;
import static in.projecteka.gateway.link.discovery.Constants.TRANSACTION_ID;
import static in.projecteka.gateway.link.discovery.Constants.X_CM_ID;
import static in.projecteka.gateway.link.discovery.Constants.X_HIP_ID;

@Component
public class DiscoveryValidator {
    @Autowired
    BridgeRegistry bridgeRegistry;

    @Autowired
    CMRegistry cmRegistry;

    @Autowired
    DiscoveryServiceClient discoveryServiceClient;

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryValidator.class);

    Mono<Tuple2<ValidatedDiscoverRequest, Boolean>> validateDiscoverRequest(HttpEntity<String> requestEntity) {
        List<String> xHipIds = requestEntity.getHeaders().get(X_HIP_ID);
        if (xHipIds==null || xHipIds.isEmpty()) {
            return errorNotify(requestEntity, TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("X-HIP-ID missing on headers").build()).thenReturn(Tuples.of(null,Boolean.FALSE));
        }
        String xHipId = xHipIds.get(0);
        Optional<YamlRegistryMapping> hipConfig = bridgeRegistry.getConfigFor(xHipId, ServiceType.HIP);
        if (hipConfig.isEmpty()) {
            return errorNotify(requestEntity, TEMP_CM_ID, Error.builder().code(ErrorCode.UNKNOWN_ERROR_OCCURRED).message("No mapping found for X-HIP-ID").build()).thenReturn(Tuples.of(null,Boolean.FALSE));
        }
        return Mono.just(Tuples.of(new ValidatedDiscoverRequest(hipConfig.get()),Boolean.TRUE));
    }

    public Mono<Void> errorNotify(HttpEntity<String> requestEntity, String cmId, Error error) {
        return Utils.deserializeRequest(requestEntity)
                .map(deserializedRequest -> Tuples.of((String)deserializedRequest.get(REQUEST_ID),(String)deserializedRequest.get(TRANSACTION_ID)))
                .filter(tuple -> {
                    String requestId = tuple.getT1();
                    String transactionId = tuple.getT2();
                    Predicate<String> isNullOrEmpty = (String value) -> value == null || value.isEmpty();
                    if (isNullOrEmpty.test(requestId)) {
                        logger.error("RequestId is empty");
                    }
                    if (isNullOrEmpty.test(transactionId)) {
                        logger.error("TransactionId is empty");
                    }
                    return !isNullOrEmpty.test(requestId) && !isNullOrEmpty.test(transactionId);
                })
                .map(tuple -> PatientDiscoveryResult.builder().error(error)
                        .requestId(UUID.fromString(tuple.getT1()))
                        .transactionId(UUID.fromString(tuple.getT2()))
                        .build()).flatMap(errorResult -> {
                    YamlRegistryMapping cmRegistryMapping = cmRegistry.getConfigFor(cmId).get();//TODO checkback when cmid is dynamic
                    return discoveryServiceClient.patientErrorResultNotify(errorResult,cmRegistryMapping.getHost());
                });
    }

    public Mono<Tuple2<ValidatedDiscoverResponse,Boolean>> validateDiscoverResponse(HttpEntity<String> requestEntity) {
        List<String> xCmIds = requestEntity.getHeaders().get(X_CM_ID);
        if (xCmIds==null || xCmIds.isEmpty()) {
            logger.error("No X-CM-ID found on Headers");
            return Mono.just(Tuples.of(null,Boolean.FALSE));
        }
        String xCmId = xCmIds.get(0);
        Optional<YamlRegistryMapping> cmConfig = cmRegistry.getConfigFor(xCmId);
        if (cmConfig.isEmpty()) {
            logger.error("No mapping found for X-CM-ID : {}",xCmId);
            return Mono.just(Tuples.of(null,Boolean.FALSE));
        }
        return Mono.just(Tuples.of(new ValidatedDiscoverResponse(cmConfig.get()),Boolean.TRUE));
    }
}
