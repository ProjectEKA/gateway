package in.projecteka.gateway.common;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.common.Utils.updateRequestId;

@AllArgsConstructor
public class ResponseOrchestrator {
    Validator validator;
    ValidatedResponseAction validatedResponseAction;

    public Mono<Void> processResponse(HttpEntity<String> maybeResponse, String routingKey) {
        return validator.validateResponse(maybeResponse, routingKey)
                .doOnSuccess(this::offloadThis)
                .then();
    }

    private void offloadThis(ValidatedResponse response) {
        Mono.defer(() -> {
            var updatedJsonNode = updateRequestId(response.getDeSerializedJsonNode(), response.getCallerRequestId());
            return validatedResponseAction.execute(response.getId(), updatedJsonNode);
        }).subscribe();
    }
}
