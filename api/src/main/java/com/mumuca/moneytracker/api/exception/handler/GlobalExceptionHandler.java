package com.mumuca.moneytracker.api.exception.handler;

import com.mumuca.moneytracker.api.exception.*;
import com.mumuca.moneytracker.api.exception.dto.APIErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

import static com.mumuca.moneytracker.api.util.HttpUtils.buildErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
        System.out.println(ex.getHttpInputMessage());
        ex.printStackTrace();
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Malformed JSON request.",
                "The request has formatting errors."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIErrorResponse<String>> handleException(Exception ex) {
        ex.printStackTrace();
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error.",
                "An unexpected error occurred."
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIErrorResponse<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found.",
                ex.getMessage()
        );
    }

    @ExceptionHandler(DifferentCurrenciesException.class)
    public ResponseEntity<APIErrorResponse<String>> handleDifferentCurrenciesException(DifferentCurrenciesException ex) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Different currencies.",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ResourceAlreadyActiveException.class)
    public ResponseEntity<APIErrorResponse<String>> handleResourceAlreadyActiveException(ResourceAlreadyActiveException ex) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Resource already active.",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ResourceAlreadyArchivedException.class)
    public ResponseEntity<APIErrorResponse<String>> handleResourceAlreadyArchivedException(ResourceAlreadyArchivedException ex) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Resource already archived.",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ResourceIsArchivedException.class)
    public ResponseEntity<APIErrorResponse<String>> handleResourceIsArchivedException(ResourceIsArchivedException ex) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Resource is archived.",
                ex.getMessage()
        );
    }
}
