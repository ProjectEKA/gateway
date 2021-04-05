package in.projecteka.gateway.clients;

import com.fasterxml.jackson.databind.JsonNode;
import in.projecteka.gateway.clients.model.Session;
import in.projecteka.gateway.common.model.GrantType;
import in.projecteka.gateway.session.SessionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Properties;

import static in.projecteka.gateway.clients.ClientError.invalidRequest;
import static in.projecteka.gateway.clients.ClientError.unableToConnect;
import static in.projecteka.gateway.clients.ClientError.unknownUnAuthorizedError;
import static reactor.core.publisher.Mono.error;

public class IdentityServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceClient.class);
    private final WebClient webClient;
    private final String realm;

    public Mono<Session> getTokenFor(String clientId, String clientSecret) {
        return getToken(loginRequestWith(clientId, clientSecret));
    }

    public Mono<Session> getTokenFor(SessionRequest request) {
        MultiValueMap<String, String> formData = loginRequestWith(request.getClientId(), request.getClientSecret());
        if (request.getGrantType() == GrantType.REFRESH_TOKEN){
            formData.set("grant_type", request.getGrantType().getValue());
            if(!StringUtils.isEmpty(request.getRefreshToken())) {
                formData.add("refresh_token", request.getRefreshToken());
            }
        } else if(request.getGrantType() == GrantType.NONE && !StringUtils.isEmpty(request.getRefreshToken())){
            return error(invalidRequest("Please mention grant type"));
        }
        return getToken(formData);
    }

    public IdentityServiceClient(WebClient.Builder webClientBuilder, String baseUrl, String realm) {
        this.realm = realm;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    private Mono<Session> getToken(MultiValueMap<String, String> formData) {
        return webClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/{realm}/protocol/openid-connect/token").build(Map.of("realm", realm)))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401,
                        clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                .flatMap(keyCloakError -> {
                                    logger.error(keyCloakError.getError(), keyCloakError);
                                    return error(unknownUnAuthorizedError(keyCloakError.getErrorDescription()));
                                }))
                .onStatus(httpStatus -> httpStatus.value() == 400,
                        clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                .flatMap(keyCloakError -> {
                                    logger.error(keyCloakError.getError(), keyCloakError);
                                    return error(invalidRequest(keyCloakError.getErrorDescription()));
                                }))
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .doOnNext(properties -> logger.error("Error Status Code: {} and error: {} ",
                                clientResponse.statusCode(),
                                properties))
                        .then(error(unableToConnect())))
                .bodyToMono(Session.class)
                .publishOn(Schedulers.elastic());
    }

    public Mono<JsonNode> certs() {
        return webClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/{realm}/protocol/openid-connect/certs").build(Map.of("realm", realm)))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .doOnNext(properties -> logger.error("Error Status Code: {} and error: {} ",
                                clientResponse.statusCode(),
                                properties))
                        .then(error(unableToConnect())))
                .bodyToMono(JsonNode.class)
                .publishOn(Schedulers.elastic());
    }

    private MultiValueMap<String, String> loginRequestWith(String clientId, String clientSecret) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "client_credentials");
        formData.add("scope", "openid");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        return formData;
    }

    public Mono<Session> getUserToken(String clientId, String clientSecret, String username, String password) {
        var formData = loginRequestWith(clientId, clientSecret);
        formData.add("username", username);
        formData.add("password", password);
        formData.set("grant_type", "password");
        return getToken(formData);
    }
}

