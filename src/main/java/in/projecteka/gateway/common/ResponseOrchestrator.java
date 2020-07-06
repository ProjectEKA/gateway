package in.projecteka.gateway.common;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static in.projecteka.gateway.common.Constants.*;
import static in.projecteka.gateway.common.Utils.updateRequestId;
import static net.logstash.logback.argument.StructuredArguments.keyValue;


@AllArgsConstructor
public class ResponseOrchestrator {
    Validator validator;
    ValidatedResponseAction validatedResponseAction;
    private static final Logger logger = LoggerFactory.getLogger(ResponseOrchestrator.class);

    public Mono<Void> processResponse(HttpEntity<String> maybeResponse, String routingKey) {
        StringBuilder apiCalled = new StringBuilder("");
        return Mono.subscriberContext().flatMap(context -> {
            apiCalled.append((String) context.get("apiCalled"));
            return validator.validateResponse(maybeResponse, routingKey);
        })
                .doOnSuccess(validatedResponse -> offloadThis(validatedResponse, routingKey,apiCalled.toString()))
                .then();
    }

    private void offloadThis(ValidatedResponse response, String routingKey, String apiCalled) {
        Mono.defer(() -> {
            Map<String,String> nameMap = new HashMap<>();
            nameMap.put(X_HIU_ID,"HIU");
            nameMap.put(X_CM_ID,"CM");
            nameMap.put(X_HIP_ID,"HIP");
            var updatedJsonNode = updateRequestId(response.getDeSerializedJsonNode(), response.getCallerRequestId());
                    logger.info("", keyValue("requestId", response.getCallerRequestId())
                    , keyValue("target", nameMap.get(routingKey))
                    , keyValue("targetId", response.getId())
                    ,keyValue("apiCalled",apiCalled));
            return validatedResponseAction.execute(response.getId(), updatedJsonNode, routingKey);
        }).subscribe();
    }
}
