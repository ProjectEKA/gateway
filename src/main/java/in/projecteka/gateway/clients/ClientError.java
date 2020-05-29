package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorRepresentation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static in.projecteka.gateway.clients.model.ErrorCode.INVALID_TOKEN;
import static in.projecteka.gateway.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
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

    public static ClientError idMissingInHeader(String id) {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, String.format("%s missing in headers", id))));
    }

    public static ClientError mappingNotFoundForId(String id) {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, String.format("No mapping found for %s", id))));
    }


    public static ClientError unAuthorized() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_TOKEN, "Token verification failed")));
    }
}
