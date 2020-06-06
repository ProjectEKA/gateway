package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ResponseOrchestrator {
    Validator validator;
    ValidatedResponseAction validatedResponseAction;

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
