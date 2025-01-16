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
}
