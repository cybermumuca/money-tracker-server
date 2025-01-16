package com.mumuca.moneytracker.api.exception.handler;

import com.mumuca.moneytracker.api.exception.dto.APIErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private <T> ResponseEntity<APIErrorResponse<T>> buildErrorResponse(
            HttpStatus status,
            String title,
            T details
    ) {
        APIErrorResponse<T> errorResponse = new APIErrorResponse<>(
                status.value(),
                LocalDateTime.now(),
                title,
                details
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIErrorResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> ((FieldError) error).getField(),
                        DefaultMessageSourceResolvable::getDefaultMessage
                ));

        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation failed.",
                validationErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIErrorResponse<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Malformed JSON request.",
                "The request has formatting errors."
        );
    }
}
