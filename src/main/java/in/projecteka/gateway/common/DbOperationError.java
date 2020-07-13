package in.projecteka.gateway.common;

import in.projecteka.gateway.clients.model.ErrorRepresentation;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import in.projecteka.gateway.clients.model.Error;

import static in.projecteka.gateway.clients.model.ErrorCode.DB_OPERATION_FAILED;

@Getter
@ToString
public class DbOperationError extends Throwable {
    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;

    public DbOperationError(String errorMessage) {
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = new ErrorRepresentation(new Error(DB_OPERATION_FAILED, errorMessage));
    }
}
