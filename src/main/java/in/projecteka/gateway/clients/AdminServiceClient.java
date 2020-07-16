package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.ClientRepresentation;
import in.projecteka.gateway.clients.model.RealmRole;
import in.projecteka.gateway.clients.model.ServiceAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static in.projecteka.gateway.clients.ClientError.clientAlredyExists;
import static in.projecteka.gateway.clients.ClientError.notFound;
import static in.projecteka.gateway.clients.ClientError.unableToConnect;
import static in.projecteka.gateway.clients.ClientError.unknownUnAuthorizedError;

public class AdminServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminServiceClient.class);

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

    public AdminServiceClient(WebClient.Builder webClientBuilder, String baseUrl, String realm, Supplier<Mono<String>> tokenGenerator) {
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
                                uriBuilder.path("/admin/realms/{realm}/clients").build(Map.of("realm", realm)))
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(clientRepresentation))
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(unknownUnAuthorizedError(keyCloakError.getErrorDescription()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 409,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(clientAlredyExists(keyCloakError.getErrorMessage()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> {
                            logger.error(clientResponse.statusCode().toString(), "Something went wrong");
                            return Mono.error(unableToConnect());
                        })
                        .toBodilessEntity())
                .then();
    }

    public Mono<ServiceAccount> getServiceAccount(String id) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/clients/{id}/service-account-user")
                                        .build(Map.of("realm", realm, "id", id)))
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(unknownUnAuthorizedError(keyCloakError.getErrorDescription()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(notFound(keyCloakError.getErrorMessage()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> {
                            logger.error(clientResponse.statusCode().toString(), "Something went wrong");
                            return Mono.error(unableToConnect());
                        })
                        .bodyToMono(ServiceAccount.class));
    }

    public Mono<List<RealmRole>> getAvailableRealmRoles(String serviceAccountId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/users/{serviceAccountId}/role-mappings/realm/available")
                                        .build(Map.of("realm", realm, "serviceAccountId", serviceAccountId)))
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(unknownUnAuthorizedError(keyCloakError.getErrorDescription()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(notFound(keyCloakError.getErrorMessage()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> {
                            logger.error(clientResponse.statusCode().toString(), "Something went wrong");
                            return Mono.error(unableToConnect());
                        })
                        .bodyToFlux(RealmRole.class)
                        .collectList());
    }

    public Mono<Void> assignRoleToClient(List<RealmRole> realmRoles, String serviceAccountId) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/users/{serviceAccountId}/role-mappings/realm")
                                        .build(Map.of("realm", realm, "serviceAccountId", serviceAccountId)))
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(realmRoles))
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(unknownUnAuthorizedError(keyCloakError.getErrorDescription()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(notFound(keyCloakError.getErrorMessage()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> {
                            logger.error(clientResponse.statusCode().toString(), "Something went wrong");
                            return Mono.error(unableToConnect());
                        })
                        .toBodilessEntity())
                .then();
    }

    public Mono<Void> deleteClient(String id) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .delete()
                        .uri(uriBuilder ->
                                uriBuilder.path("/admin/realms/{realm}/clients/{id}")
                                        .build(Map.of("realm", realm, "id", id)))
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 401,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(unknownUnAuthorizedError(keyCloakError.getErrorDescription()));
                                        }))
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> clientResponse.bodyToMono(KeyCloakError.class)
                                        .flatMap(keyCloakError -> {
                                            logger.error(keyCloakError.getError(), keyCloakError);
                                            return Mono.error(notFound(keyCloakError.getErrorMessage()));
                                        }))
                        .onStatus(HttpStatus::isError, clientResponse -> {
                            logger.error(clientResponse.statusCode().toString(), "Something went wrong");
                            return Mono.error(unableToConnect());
                        })
                        .toBodilessEntity())
                .then();
    }
}
