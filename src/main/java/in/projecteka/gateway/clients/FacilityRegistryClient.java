package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.FacilitySearchResponse;
import in.projecteka.gateway.clients.model.FindFacilityByIDResponse;
import in.projecteka.gateway.clients.model.Session;
import in.projecteka.gateway.common.cache.CacheAdapter;
import in.projecteka.gateway.registry.FacilityRegistryProperties;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.UUID;

import static in.projecteka.gateway.clients.ClientError.unableToConnect;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.error;


public class FacilityRegistryClient {
    private static final Logger logger = LoggerFactory.getLogger(FacilityRegistryClient.class);
    public static final String FACILITY_SEARCH_INCLUDE_PHOTO = "N"; //"N" for no, "Y" for yes
    public static final String FACILITY_TOKEN_CACHE_KEY = "facilityRegistry:accessToken";

    private final WebClient registryWebClient;
    private final WebClient authWebClient;
    private final FacilityRegistryProperties properties;
    private final CacheAdapter<String, String> facilityTokenCache;


    public FacilityRegistryClient(WebClient.Builder webClientBuilder, FacilityRegistryProperties properties,
                                  CacheAdapter<String, String> facilityTokenCache) {
        this.registryWebClient = webClientBuilder.baseUrl(properties.getUrl()).build();
        this.authWebClient = webClientBuilder.baseUrl(properties.getAuthUrl()).build();
        this.properties = properties;
        this.facilityTokenCache = facilityTokenCache;
    }

    private Mono<String> getTokenForFacilityRegistry() {
        return authWebClient
                .post()
                .uri("/sessions")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestWith(properties.getClientId(), properties.getClientSecret())))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .doOnNext(logger::error).then(Mono.error(ClientError.unknownUnAuthorizedError("Unable to get token for facility registry"))))
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .doOnNext(logger::error)
                        .then(Mono.error(ClientError.unableToConnect())))
                .bodyToMono(Session.class)
                .flatMap(session -> facilityTokenCache.put(FACILITY_TOKEN_CACHE_KEY, session.getAccessToken())
                        .thenReturn(session.getAccessToken()))
                .doOnSubscribe(subscription -> logger.info("About to get token for facility registry"));
    }

    public Mono<String> getToken() {
        return facilityTokenCache.get(FACILITY_TOKEN_CACHE_KEY)
                .switchIfEmpty(getTokenForFacilityRegistry())
                .map(token -> String.format("%s %s", "Bearer", token));
    }

    public Mono<FacilitySearchResponse> searchFacilityByName(String name, String state, String district) {
        return getToken()
                .flatMap(token -> registryWebClient.post()
                        .uri("/v1.0/facility/search-facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .body(BodyInserters.fromValue(searchByNameRequest(name, state, district)))
                        .retrieve()
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error("Error Status Code: {} and error: {} ",
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .bodyToMono(FacilitySearchResponse.class));
    }

    public Mono<FindFacilityByIDResponse> getFacilityById(String facilityId) {
        return getToken()
                .flatMap(token -> registryWebClient.post()
                        .uri("/v1.0/facility/fetch-facility-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .body(BodyInserters.fromValue(getFacilityByIdRequest(facilityId)))
                        .retrieve()
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error("Error Status Code: {} and error: {} ",
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .bodyToMono(FindFacilityByIDResponse.class));
    }

    private HashMap<String, Object> searchByNameRequest(String name, String state, String district) {
        var requestData = new HashMap<String, Object>();
        var facilityInfo = new HashMap<String, String>();
        facilityInfo.put("facilityName", name);
        facilityInfo.put("photo", FACILITY_SEARCH_INCLUDE_PHOTO);

        if (!StringUtils.isEmpty(state)) {
            facilityInfo.put("state", state);
        }
        if (!StringUtils.isEmpty(district)) {
            facilityInfo.put("district", district);
        }

        requestData.put("requestId", UUID.randomUUID().toString());
        requestData.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        requestData.put("facility", facilityInfo);
        return requestData;
    }

    private HashMap<String, Object> getFacilityByIdRequest(String facilityId) {
        var requestData = new HashMap<String, Object>();
        var facilityInfo = new HashMap<String, String>();
        facilityInfo.put("id", facilityId);
        requestData.put("requestId", UUID.randomUUID().toString());
        requestData.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        requestData.put("facility", facilityInfo);
        return requestData;
    }

    private FacilityRegistryClient.SessionRequest requestWith(String clientId, String clientSecret) {
        return new FacilityRegistryClient.SessionRequest(clientId, clientSecret);
    }

    @AllArgsConstructor
    @Value
    private static class SessionRequest {
        String clientId;
        String clientSecret;
    }
}
