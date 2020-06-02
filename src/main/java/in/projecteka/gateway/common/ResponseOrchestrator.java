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
    public Mono<Void> processResponse(HttpEntity<String> requestEntity, String id) {
        validator.validateResponse(requestEntity, id)
                .flatMap(validRequest -> {
                    JsonNode updatedJsonNode = Utils.updateRequestId(validRequest.getDeserializedJsonNode(),
                            validRequest.getCallerRequestId());
                    return validatedResponseAction.execute(id, validRequest.getId(), updatedJsonNode);
                }).subscribe();
        return Mono.empty();
    }
}
