package in.projecteka.gateway.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public class Serializer {
    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);
    public static final String ERROR_IN_DE_SERIALISE = "Error while de-serialise";
    public static final String ERROR_IN_SERIALIZING_REQUEST_BODY = "Error while serializing request body";
    static ObjectMapper objectMapper = new ObjectMapper(); //TODO

    private Serializer() {
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

    public static Mono<JsonNode> deserializeRequestAsJsonNode(HttpEntity<String> requestEntity) {
        try {
            return Mono.just(objectMapper.readValue(requestEntity.getBody(), JsonNode.class));
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

    public static <T> Optional<String> from(T value) {
        try {
            return Optional.of(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            logger.error(ERROR_IN_SERIALIZING_REQUEST_BODY, e);
            return Optional.empty();
        }
    }
}
