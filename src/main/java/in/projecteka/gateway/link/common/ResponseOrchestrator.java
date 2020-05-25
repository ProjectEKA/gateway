package in.projecteka.gateway.link.common;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.ServiceClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ResponseOrchestrator <T extends ServiceClient>{
    Validator validator;

    T serviceClient;

    private static final Logger logger = LoggerFactory.getLogger(ResponseOrchestrator.class);
    public Mono<Void> processResponse(HttpEntity<String> requestEntity) {
        return validator.validateResponse(requestEntity)
                .flatMap(validRequest -> {
                    JsonNode updatedJsonNode = Utils.updateRequestId(validRequest.getDeserializedJsonNode(),
                            validRequest.getCallerRequestId());
                    return serviceClient
                            .routeResponse(updatedJsonNode,validRequest.getCmConfig().getHost())
                            .onErrorResume(throwable -> {
                                //Does it make sense to call the same API back to notify only Error?
                                logger.error("Error in notifying CM with result",throwable);
                                return Mono.empty();
                            });
                });
    }
}
