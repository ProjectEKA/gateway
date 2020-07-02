package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

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
//        MDC.put("request-id", (String) requestBody.get("requestId"));
//        MDC.put("method", "POST");
//        MDC.put("path", path);
//        MDC.put("source", getServiceName(source));
//        MDC.put("source-id", clientId);
//        MDC.put("target", getServiceName(target));
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

//    public static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
//        return signal -> {
//            if (!signal.isOnNext()) return;
//            Optional<String> toPutInMdc = signal.getContext().getOrEmpty("CONTEXT_KEY");
//
//            toPutInMdc.ifPresentOrElse(keyValue -> {
//                try (MDC.MDCCloseable closeable = MDC.putCloseable("CONTEXT_KEY", keyValue)) {
//                    logStatement.accept(signal.get());
//                }
//            }, () -> logStatement.accept(signal.get()));
//        };
//    }

    public static Consumer<Signal<String>> logWithContext(HttpEntity<String> requestEntity, Consumer<HttpEntity<String>> logAction) {
        JSONObject requestBody = (JSONObject) JSONValue.parse(requestEntity.getBody());
//        MDC.put("request-id", (String) requestBody.get("requestId"));
//        MDC.put("method", "POST");
//        MDC.put("path", path);
//        MDC.put("source", source);
//        MDC.put("target", getServiceName(target));
//        MDC.put("target-id", requestEntity.getHeaders().getFirst(target));

        return s -> {
            try {
//            request.forEach((name, values) -> MDC.put(name, values.get(0)));
                MDC.put("request-id", (String) requestBody.get("requestId"));
                MDC.put("method", "POST");
                MDC.put("path", "/fdfsdfsdfsd");
                logAction.accept(requestEntity);
            } finally {
                MDC.remove("request-id");
                MDC.remove("method");
                MDC.remove("path");
            }
        };
    }

}
