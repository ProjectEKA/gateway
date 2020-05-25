package in.projecteka.gateway.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.gateway.common.CentralRegistry;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.link.discovery.model.PatientDiscoveryResult;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@AllArgsConstructor
public class DiscoveryServiceClient {
    private ServiceOptions serviceOptions;
    private WebClient.Builder webClientBuilder;
    private ObjectMapper objectMapper;
    private CentralRegistry centralRegistry;
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServiceClient.class);

    public Mono<Void> patientFor(Map<String, Object> request, String url) {
        return centralRegistry.authenticate()
                .flatMap(token -> serializeRequest(request)
                        .flatMap(serializedRequest ->
                                webClientBuilder.build()
                                        .post()
                                        .uri(url + "/care-contexts/discover")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .bodyValue(serializedRequest)
                                        .retrieve()
                                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                                clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                                        .bodyToMono(Void.class)
                                        .timeout(Duration.ofSeconds(serviceOptions.getTimeout()))));
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

    private Mono<Object> serializeRequest(PatientDiscoveryResult request) {
        try {
            return Mono.just(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            logger.error("Error in serializing request body", e);
            return Mono.empty();
        }
    }

    public Mono<Void> patientErrorResultNotify(PatientDiscoveryResult request, String cmUrl) {
        return centralRegistry.authenticate()
                .flatMap(token -> serializeRequest(request)
                .flatMap(serializeRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(cmUrl + "/care-contexts/on-discover")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .bodyValue(request)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                .bodyToMono(Void.class)));
    }

    public Mono<Void> patientDiscoveryResultNotify(JsonNode request, String cmUrl) {
        return centralRegistry.authenticate()
                .flatMap(token -> serializeRequest(request)
                .flatMap(serializedRequest ->
                        webClientBuilder.build()
                                .post()
                                .uri(cmUrl + "/care-contexts/on-discover")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .bodyValue(serializedRequest)
                                .retrieve()
                                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                        clientResponse -> Mono.error(ClientError.unableToConnect()))//TODO Error handling
                                .bodyToMono(Void.class)
                                .timeout(Duration.ofSeconds(serviceOptions.getTimeout()))));
    }
}