package in.projecteka.gateway.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.link.discovery.model.PatientDiscoveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class DiscoveryServiceClient {
    ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServiceClient.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    public Mono<Void> patientFor(Map<String, Object> request, String url) {
        return serializeRequest(request)
                .flatMap(serializedRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(url + "/patients/care-contexts/discover")
                                .bodyValue(serializedRequest)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                                .bodyToMono(Void.class));
    }

    private Mono<String> serializeRequest(Map<String, Object> request) {
        try {
            return Mono.just(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            logger.error("Error in serializing request body", e);
            return Mono.empty();
        }
    }

    private Mono<String> serializeRequest(JsonNode jsonNode) {
        try {
            return Mono.just(objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            logger.error("Error in serializing request body", e);
            return Mono.empty();
        }
    }

    public Mono<Void> patientErrorResultNotify(PatientDiscoveryResult request, String cmUrl) {
        return webClientBuilder.build()
                .post()
                .uri(cmUrl + "/patients/care-contexts/on-discover")
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                .bodyToMono(Void.class);
    }

    public Mono<Void> patientDiscoveryResultNotify(JsonNode request, String cmUrl) {
        return serializeRequest(request)
                .flatMap(serializedRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(cmUrl + "/patients/care-contexts/on-discover")
                                .bodyValue(serializedRequest)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                                .bodyToMono(Void.class));
    }
}