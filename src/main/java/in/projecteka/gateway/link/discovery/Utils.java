package in.projecteka.gateway.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;

import java.util.Map;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    static ObjectMapper objectMapper = new ObjectMapper();//TODO

    private Utils() {}

    public static Mono<Map<String, Object>> deserializeRequest(HttpEntity<String> requestEntity) {
        try {
            return Mono.just(objectMapper.readValue(requestEntity.getBody(), new TypeReference<>() { }));
        } catch (JsonProcessingException e) {
            logger.error("Error in deserializing", e);
            return Mono.empty();
        }
    }

    public static Mono<JsonNode> deserializeRequestAsJsonNode(HttpEntity<String> requestEntity) {
        try {
            return Mono.just(objectMapper.readValue(requestEntity.getBody(),JsonNode.class));
        } catch (JsonProcessingException e) {
            logger.error("Error in deserializing", e);
            return Mono.empty();
        }
    }

    public static Mono<String> serializeRequest(JsonNode jsonNode) {
        try {
            return Mono.just(objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            logger.error("Error in serializing request body", e);
            return Mono.empty();
        }
    }
}
