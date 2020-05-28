package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ResponseOrchestrator {
    Validator validator;

    ValidatedResponseAction validatedResponseAction;

    private static final Logger logger = LoggerFactory.getLogger(ResponseOrchestrator.class);
    public Mono<Void> processResponse(HttpEntity<String> requestEntity) {
        return validator.validateResponse(requestEntity)
                .flatMap(validRequest -> {
                    JsonNode updatedJsonNode = Utils.updateRequestId(validRequest.getDeserializedJsonNode(),
                            validRequest.getCallerRequestId());
                    return validatedResponseAction.execute(validRequest.getXCmId(),updatedJsonNode);
                });
    }
}
