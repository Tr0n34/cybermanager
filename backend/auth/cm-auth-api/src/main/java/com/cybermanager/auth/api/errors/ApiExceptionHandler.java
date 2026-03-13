package com.cybermanager.auth.api.errors;

import com.cybermanager.auth.application.services.AuthenticationFailedException;
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

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationFailedException exception) {
        LOGGER.warn("Authentication error handled message={}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        LOGGER.warn("Authentication bad request handled message={}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnhandled(Exception exception) {
        LOGGER.error("Unhandled authentication server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("Unexpected server error", Instant.now()));
    }
}

