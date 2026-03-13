package com.cybermanager.api.errors;

import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.services.users.UserManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException exception) {
        HttpStatus status = toStatus(exception.type());
        LOGGER.warn(
                "Business error handled type={} code={} status={} message={}",
                exception.type(),
                exception.code(),
                status.value(),
                exception.getMessage()
        );
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(exception.code(), exception.getMessage(), status.value(), Instant.now()));
    }

    @ExceptionHandler(UserManagementException.class)
    ResponseEntity<ApiErrorResponse> handleUserBusiness(UserManagementException exception) {
        return handleBusiness((BusinessException) exception);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        LOGGER.warn("Bad request handled message={}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("BAD_REQUEST", exception.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnhandled(Exception exception) {
        LOGGER.error("Unhandled server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR.value(), Instant.now()));
    }

    private HttpStatus toStatus(BusinessErrorType type) {
        return switch (type) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }
}

