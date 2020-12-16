package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.ClientRepresentation;
import in.projecteka.gateway.clients.model.ClientSecret;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.clients.model.ServiceAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static in.projecteka.gateway.clients.ClientError.clientAlreadyExists;
import static in.projecteka.gateway.clients.ClientError.notFound;
import static in.projecteka.gateway.clients.ClientError.unableToConnect;
import static in.projecteka.gateway.clients.ClientError.unknownUnAuthorizedError;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

public class AdminServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminServiceClient.class);
    private static final String REALM_NAME = "realm";
    private static final String ERROR_MESSAGE = "Error Status Code: {} and error: {}";

    private static final ClientRepresentation.ClientRepresentationBuilder clientRepresentation
            = ClientRepresentation.builder()
            .redirectUris(List.of("http://localhost:9007"))
            .surrogateAuthRequired(false)
            .enabled(true)
            .alwaysDisplayInConsole(false)
            .clientAuthenticatorType("client-secret")
            .notBefore(0)
            .bearerOnly(false)
            .consentRequired(false)
            .standardFlowEnabled(true)
            .implicitFlowEnabled(false)
            .directAccessGrantsEnabled(true)
            .serviceAccountsEnabled(true)
            .publicClient(false)
            .frontchannelLogout(false)
            .protocol("openid-connect")
            .fullScopeAllowed(true)
            .authorizationServicesEnabled(true);
    private final WebClient webClient;
    private final String realm;
    private final Supplier<Mono<String>> tokenGenerator;

    public AdminServiceClient(WebClient.Builder webClientBuilder,
                              String baseUrl,
                              String realm,
                              Supplier<Mono<String>> tokenGenerator) {
        this.realm = realm;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.tokenGenerator = tokenGenerator;
    }

    public Mono<Void> createClient(String clientId) {
        var build = clientRepresentation.clientId(clientId).id(clientId).build();
        return createClient(build);
    }

    private Mono<Void> createClient(ClientRepresentation clientRepresentation) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/clients").build(Map.of(REALM_NAME, realm)))
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .body(BodyInserters.fromValue(clientRepresentation))
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(unknownUnAuthorizedError(keyCloakError.getError()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 409,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getErrorMessage(), keyCloakError);
                                            return error(clientAlreadyExists(keyCloakError.getErrorMessage()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error(ERROR_MESSAGE,
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .toBodilessEntity())
                .then();
    }

//    public Mono<String> getId(String clientId) {
//        return tokenGenerator.get()
//                .flatMap(token -> webClient
//                        .get()
//                        .uri(uriBuilder ->
//                                uriBuilder.path("/admin/realms/{realm}/clients/{clientId}")
//                                        .build(Map.of(REALM_NAME, realm, "clientId", clientId)))
//                        .header(AUTHORIZATION, token)
//                        .accept(APPLICATION_JSON)
//                        .retrieve()
//                        .onStatus(httpStatus -> httpStatus.value() == 401,
//                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
//                                        .flatMap(keyCloakError -> {
//                                            logger.error(keyCloakError.getError(), keyCloakError);
//                                            return error(unknownUnAuthorizedError(keyCloakError.getError()));
//                                        }))
//                        .onStatus(httpStatus -> httpStatus.value() == 404,
//                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
//                                        .flatMap(keyCloakError -> {
//                                            logger.error(keyCloakError.getError(), keyCloakError);
//                                            return error(notFound(keyCloakError.getError()));
//                                        }))
//                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
//                                .doOnNext(properties -> logger.error(ERROR_MESSAGE,
//                                        clientResponse.statusCode(),
//                                        properties))
//                                .then(error(unableToConnect())))
//                        .bodyToMono(Client.class));
//    }

    public Mono<ServiceAccount> getServiceAccount(String id) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/clients/{id}/service-account-user")
                                        .build(Map.of(REALM_NAME, realm, "id", id)))
                        .header(AUTHORIZATION, token)
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(unknownUnAuthorizedError(keyCloakError.getError()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(notFound(keyCloakError.getError()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error(ERROR_MESSAGE,
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .bodyToMono(ServiceAccount.class));
    }

    public Mono<List<RealmRole>> getAvailableRealmRoles(String serviceAccountId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/users/{serviceAccountId}/role-mappings/realm/available")
                                        .build(Map.of(REALM_NAME, realm, "serviceAccountId", serviceAccountId)))
                        .header(AUTHORIZATION, token)
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(unknownUnAuthorizedError(keyCloakError.getError()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(notFound(keyCloakError.getError()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error(ERROR_MESSAGE,
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .bodyToFlux(RealmRole.class)
                        .collectList());
    }

    public Mono<Void> assignRoleToClient(List<RealmRole> realmRoles, String serviceAccountId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/users/{serviceAccountId}/role-mappings/realm")
                                        .build(Map.of(REALM_NAME, realm, "serviceAccountId", serviceAccountId)))
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .body(BodyInserters.fromValue(realmRoles))
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(unknownUnAuthorizedError(keyCloakError.getError()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(notFound(keyCloakError.getError()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error(ERROR_MESSAGE,
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .toBodilessEntity())
                .then();
    }

    public Mono<Void> deleteClient(String id) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .delete()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/clients/{id}")
                                        .build(Map.of(REALM_NAME, realm, "id", id)))
                        .header(AUTHORIZATION, token)
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(unknownUnAuthorizedError(keyCloakError.getError()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(notFound(keyCloakError.getError()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error(ERROR_MESSAGE,
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .toBodilessEntity())
                .then();
    }

    public Mono<ClientSecret> getClientSecret(String clientId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/clients/{client_id}/client-secret")
                                        .build(Map.of(REALM_NAME, realm, "client_id", clientId)))
                        .header(AUTHORIZATION, token)
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(unknownUnAuthorizedError(keyCloakError.getError()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return error(notFound(keyCloakError.getError()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(properties -> logger.error(ERROR_MESSAGE,
                                        clientResponse.statusCode(),
                                        properties))
                                .then(error(unableToConnect())))
                        .bodyToMono(ClientSecret.class));
    }

    public Mono<Void> createClientIfNotExists(String clientId) {
        return createClient(clientId)
                .onErrorResume(ClientError.class, exception -> {
                    if (exception.getHttpStatus() == CONFLICT) {
                        logger.info("Client {} already exists", clientId);
                        return empty();
                    }
                    logger.error(exception.getMessage(), exception);
                    return error(exception);
                });
    }

    public Mono<Void> deleteClientIfExists(String clientId) {
        return deleteClient(clientId)
                .onErrorResume(ClientError.class, exception -> {
                    if (exception.getHttpStatus() == NOT_FOUND) {
                        logger.info("Client {} does not exist", clientId);
                        return empty();
                    }
                    logger.error(exception.getMessage(), exception);
                    return error(exception);
                });
    }
}

