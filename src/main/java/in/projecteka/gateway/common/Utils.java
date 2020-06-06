package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.gateway.common.Constants.REQUEST_ID;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final String ERROR_IN_DE_SERIALISE = "Error while de-serialise";
    public static final String ERROR_IN_SERIALIZING_REQUEST_BODY = "Error while serializing request body";
    static ObjectMapper objectMapper = new ObjectMapper(); //TODO

    private Utils() {
    }

    public static Mono<Map<String, Object>> deserializeRequest(HttpEntity<String> requestEntity) {
        try {
            return Mono.just(objectMapper.readValue(requestEntity.getBody(), new TypeReference<>() {
            }));
        } catch (JsonProcessingException e) {
            logger.error(ERROR_IN_DE_SERIALISE, e);
            return Mono.empty();
        }
    }

    public static Optional<Map<String, Object>> from(HttpEntity<String> requestEntity) {
        try {
            return Optional.of(objectMapper.readValue(requestEntity.getBody(), new TypeReference<>() {
            }));
        } catch (Exception e) {
            logger.error(ERROR_IN_DE_SERIALISE, e);
            return Optional.empty();
        }
    }

    public static Mono<JsonNode> deserializeRequestAsJsonNode(HttpEntity<String> requestEntity) {
        try {
            return Mono.just(objectMapper.readValue(requestEntity.getBody(), JsonNode.class));
        } catch (JsonProcessingException e) {
            logger.error(ERROR_IN_DE_SERIALISE, e);
            return Mono.empty();
        }
    }

    public static Mono<String> serializeRequest(JsonNode jsonNode) {
        try {
            return Mono.just(objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            logger.error(ERROR_IN_SERIALIZING_REQUEST_BODY, e);
            return Mono.empty();
        }
    }

    public static Mono<String> serializeRequest(Map<String, Object> request) {
        try {
            return Mono.just(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            logger.error(ERROR_IN_SERIALIZING_REQUEST_BODY, e);
            return Mono.empty();
        }
    }

    public static JsonNode updateRequestId(JsonNode jsonNode, String callerRequestId) {
        ObjectNode mutableNode = (ObjectNode) jsonNode;
        mutableNode.put(REQUEST_ID, UUID.randomUUID().toString());
        ObjectNode respNode = (ObjectNode) mutableNode.get("resp");
        respNode.put(REQUEST_ID, callerRequestId);
        return jsonNode;
    }
}
