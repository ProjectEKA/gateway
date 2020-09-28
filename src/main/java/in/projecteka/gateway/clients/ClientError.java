package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorCode;
import in.projecteka.gateway.clients.model.ErrorRepresentation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_BRIDGE_SERVICE_REQUEST;
import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_CM_SERVICE_REQUEST;
import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_TOKEN;
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
    private static final String CANNOT_PROCESS_REQUEST_TRY_LATER = "Cannot process the request at the moment, " +
            "please try later.";
    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;

    public ClientError(HttpStatus httpStatus, ErrorRepresentation errorRepresentation) {
        this.httpStatus = httpStatus;
        this.error = errorRepresentation;
    }

    public static ClientError unableToConnect() {
        return internalServerError(CANNOT_PROCESS_REQUEST_TRY_LATER);
    }

    public static ClientError mappingNotFoundForId(String id) {
        return internalServerError(format("No mapping found for %s", id));
    }

    public static ClientError invalidRequest(String message) {
        return new ClientError(BAD_REQUEST, errorOf(message, UNKNOWN_ERROR_OCCURRED));
    }

    public static ClientError tooManyRequests() {
        return new ClientError(TOO_MANY_REQUESTS, errorOf("Too many requests found", TOO_MANY_REQUESTS_FOUND));
    }

    public static ClientError unknownUnAuthorizedError(String message) {
        return new ClientError(UNAUTHORIZED, errorOf(message, UNKNOWN_ERROR_OCCURRED));
    }

    public static ClientError clientAlreadyExists(String message) {
        return new ClientError(CONFLICT, errorOf(message, UNKNOWN_ERROR_OCCURRED));
    }

    public static ClientError notFound(String message) {
        return new ClientError(NOT_FOUND, errorOf(message, UNKNOWN_ERROR_OCCURRED));
    }

    public static ClientError refreshTokenNotFound() {
        return new ClientError(BAD_REQUEST, errorOf("Refresh token not found", INVALID_TOKEN));
    }

    public static ClientError invalidBridgeRegistryRequest(String message) {
        return badBridgeRequest(message);
    }

    public static ClientError invalidCMRegistryRequest() {
        return badCMRequest("consent_manager suffix and url can't be empty");
    }

    public static ClientError invalidCMEntry() {
        return badCMRequest("can't register an inactive consent_manager");
    }

    public static ClientError invalidBridgeServiceRequest() {
        return badBridgeRequest("Can't be serviced by multiple bridges");
    }

    public static ClientError unknownErrorOccurred() {
        return internalServerError("Unknown error occurred");
    }

    private static ClientError internalServerError(String message) {
        return new ClientError(INTERNAL_SERVER_ERROR, errorOf(message, UNKNOWN_ERROR_OCCURRED));
    }

    private static ClientError badBridgeRequest(String message) {
        return new ClientError(BAD_REQUEST, errorOf(message, INVALID_BRIDGE_SERVICE_REQUEST));
    }

    private static ClientError badCMRequest(String message) {
        return new ClientError(BAD_REQUEST, errorOf(message, INVALID_CM_SERVICE_REQUEST));
    }

    private static ErrorRepresentation errorOf(String message, ErrorCode errorCode) {
        return new ErrorRepresentation(new Error(errorCode, message));
    }
}
