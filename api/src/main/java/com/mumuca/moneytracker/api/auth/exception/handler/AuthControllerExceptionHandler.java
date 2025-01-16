package com.mumuca.moneytracker.api.auth.exception.handler;

import com.mumuca.moneytracker.api.auth.controller.AuthController;
import com.mumuca.moneytracker.api.auth.exception.CredentialsMismatchException;
import com.mumuca.moneytracker.api.auth.exception.UserAlreadyExistsException;
import com.mumuca.moneytracker.api.exception.dto.APIErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.mumuca.moneytracker.api.util.HttpUtils.buildErrorResponse;

@RestControllerAdvice(assignableTypes = {AuthController.class})
public class AuthControllerExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<APIErrorResponse<String>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "User already exists.",
                "The user already exists."
        );
    }

    @ExceptionHandler(CredentialsMismatchException.class)
    public ResponseEntity<APIErrorResponse<String>> handleCredentialsMismatchException(CredentialsMismatchException ex) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Credentials mismatch.",
                ex.getMessage()
        );
    }
}
