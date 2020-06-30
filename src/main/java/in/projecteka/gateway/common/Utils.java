package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;

import java.util.UUID;

import static in.projecteka.gateway.common.Constants.*;

public class Utils {

    private Utils() {

    }

    public static JsonNode updateRequestId(JsonNode jsonNode, String callerRequestId) {
        ObjectNode mutableNode = (ObjectNode) jsonNode;
        mutableNode.put(REQUEST_ID, UUID.randomUUID().toString());
        ObjectNode respNode = (ObjectNode) mutableNode.get("resp");
        respNode.put(REQUEST_ID, callerRequestId);
        return jsonNode;

    }
    public static void requestInfoLog(HttpEntity<String> requestEntity, String clientId, String source, String target, String path) {

        JSONObject requestBody = (JSONObject) JSONValue.parse(requestEntity.getBody());
        MDC.put("request-id", (String) requestBody.get("requestId"));
        MDC.put("method", "POST");
        MDC.put("path", path);
        MDC.put("source", getServiceName(source));
        MDC.put("source-id", clientId);
        MDC.put("target", getServiceName(target));
    }

    private static String getServiceName(String routingKey) {
        if (routingKey.equals(X_HIU_ID)) return "HIU";
        if (routingKey.equals(X_HIP_ID)) return "HIP";
        if (routingKey.equals(X_CM_ID)) return "CM";
        return null;
    }

    public static void responseInfoLog(HttpEntity<String> requestEntity, String source, String target, String path) {
        JSONObject requestBody = (JSONObject) JSONValue.parse(requestEntity.getBody());
        MDC.put("request-id", (String) requestBody.get("requestId"));
        MDC.put("method", "POST");
        MDC.put("path", path);
        MDC.put("source", source);
        MDC.put("target", getServiceName(target));
        MDC.put("target-id", requestEntity.getHeaders().getFirst(target));
    }
}
