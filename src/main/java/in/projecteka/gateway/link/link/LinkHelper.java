package in.projecteka.gateway.link.link;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.projecteka.gateway.clients.LinkOnInitServiceClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static in.projecteka.gateway.link.discovery.Constants.REQUEST_ID;

@AllArgsConstructor
public class LinkHelper {
    LinkValidator linkValidator;
    LinkOnInitServiceClient linkOnInitServiceClient;
    private static final Logger logger = LoggerFactory.getLogger(LinkHelper.class);

    Mono<Void> doLinkInit(HttpEntity<String> requestEntity) {
        return Mono.empty();
    }

    Mono<Void> doOnLinkInit(HttpEntity<String> requestEntity) {
        return linkValidator.validateOnLinkInit(requestEntity)
                .flatMap(validatedLinkInitResponse -> {
                    JsonNode updatedJsonNode = updateRequestId(validatedLinkInitResponse.getDeserializedJsonNode(), validatedLinkInitResponse.getCallerRequestId());
                    return linkOnInitServiceClient.
                            linkOnInitResultNotify(updatedJsonNode,validatedLinkInitResponse.getCmConfig().getHost())
                            .onErrorResume(throwable -> {
                                logger.error("Error in notifying CM with result",throwable);
                                return Mono.empty();
                            });
                });
    }

    private JsonNode updateRequestId(JsonNode jsonNode, String callerRequestId) {
        ObjectNode mutableNode = (ObjectNode) jsonNode;
        mutableNode.put(REQUEST_ID, UUID.randomUUID().toString());
        ObjectNode respNode = (ObjectNode) mutableNode.get("resp");
        respNode.put(REQUEST_ID, callerRequestId);
        return jsonNode;
    }
}
