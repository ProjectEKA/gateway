package in.projecteka.gateway.clients;

import in.projecteka.gateway.clients.model.Error;
import in.projecteka.gateway.clients.model.ErrorRepresentation;
import in.projecteka.gateway.common.DbOperationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.clients.ClientError.unknownErrorOccurred;
import static in.projecteka.gateway.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;

public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public GlobalExceptionHandler(
            ErrorAttributes errorAttributes,
            ResourceProperties resourceProperties,
            ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, applicationContext);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        var message = format("Error happened for path: %s, method: %s, message: %s",
                request.path(),
                request.method(),
                error.getMessage());
        logger.error(message, error);
        // Default error response
        HttpStatus status = INTERNAL_SERVER_ERROR;
        var bodyInserter = fromValue(unknownErrorOccurred().getError());

        if(error instanceof ResponseStatusException) {
            status = ((ResponseStatusException) error).getStatus();
        }

        if (error instanceof WebExchangeBindException) {
            WebExchangeBindException bindException = (WebExchangeBindException) error;
            FieldError fieldError = bindException.getFieldError();
            if (fieldError != null) {
                String errorMsg = fieldError.getField() + ": " + fieldError.getDefaultMessage();
                ErrorRepresentation errorRepresentation = ErrorRepresentation.builder()
                        .error(new Error(UNKNOWN_ERROR_OCCURRED, errorMsg))
                        .build();
                bodyInserter = fromValue(errorRepresentation);
                return ServerResponse.status(BAD_REQUEST).contentType(APPLICATION_JSON).body(bodyInserter);
            }
        }

        if (error instanceof ClientError) {
            status = ((ClientError) error).getHttpStatus();
            bodyInserter = fromValue(((ClientError) error).getError());
        }

        if (error instanceof DbOperationError) {
            status = ((DbOperationError) error).getHttpStatus();
            bodyInserter = fromValue(((DbOperationError) error).getError());
        }

        if (error instanceof ServerWebInputException) {
            ServerWebInputException inputException = (ServerWebInputException) error;
            status = inputException.getStatus();
        }

        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(bodyInserter);
    }
}
