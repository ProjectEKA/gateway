package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorRepresentation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_BRIDGE_REGISTRY_REQUEST;
import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_BRIDGE_SERVICE_REQUEST;
import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_CM_ENTRY;
import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_CM_SERVICE_REQUEST;
import static in.projecteka.gateway.clients.model.ErrorCode.TOO_MANY_REQUESTS_FOUND;
import static in.projecteka.gateway.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Getter
public class ClientError extends Throwable {
    private static final String CANNOT_PROCESS_REQUEST_TRY_LATER = "Cannot process the request at the moment, please " +
            "try later.";
    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;

    public ClientError(HttpStatus httpStatus, ErrorRepresentation errorRepresentation) {
        this.httpStatus = httpStatus;
        this.error = errorRepresentation;
    }

    public static ClientError unableToConnect() {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, CANNOT_PROCESS_REQUEST_TRY_LATER)));
    }

    public static ClientError mappingNotFoundForId(String id) {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, format("No mapping found for %s", id))));
    }

    public static ClientError invalidRequest(String message) {
        return new ClientError(BAD_REQUEST, new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, message)));
    }

    public static ClientError tooManyRequests() {
        return new ClientError(TOO_MANY_REQUESTS,
                new ErrorRepresentation(new Error(TOO_MANY_REQUESTS_FOUND, "Too many requests found")));
    }

    public static ClientError unknownUnAuthorizedError(String message) {
        return new ClientError(UNAUTHORIZED, new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, message)));
    }

    public static ClientError clientAlreadyExists(String message) {
        return new ClientError(CONFLICT, new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, message)));
    }

    public static ClientError notFound(String message) {
        return new ClientError(NOT_FOUND, new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, message)));
    }

    public static ClientError invalidBridgeRegistryRequest(String message) {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_BRIDGE_REGISTRY_REQUEST, message)));
    }

    public static ClientError invalidCMRegistryRequest() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_CM_SERVICE_REQUEST,
                        "consent_manager suffix and url can't be empty")));
    }

    public static ClientError invalidCMEntry() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_CM_ENTRY,
                        "can't register an inactive consent_manager")));
    }


    public static ClientError invalidBridgeServiceRequest() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_BRIDGE_SERVICE_REQUEST,
                        "Can't be serviced by multiple bridges")));
    }
}
