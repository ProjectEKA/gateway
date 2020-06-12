package in.projecteka.gateway.common;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Utils.updateRequestId;

@AllArgsConstructor
public class ResponseOrchestrator {
    Validator validator;
    ValidatedResponseAction validatedResponseAction;

    public Mono<Void> processResponse(HttpEntity<String> maybeResponse, String id) {
        return validator.validateResponse(maybeResponse, id)
                .doOnSuccess(response -> offloadThis(response, id))
                .then();
    }

    private void offloadThis(ValidatedResponse response, String id) {
        Mono.defer(() -> {
            var updatedJsonNode = updateRequestId(response.getDeserializedJsonNode(), response.getCallerRequestId());
            return validatedResponseAction.execute(id, response.getId(), updatedJsonNode);
        }).subscribe();
    }
}
