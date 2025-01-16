package com.mumuca.moneytracker.api.auth.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("User already registered with email: " + email);
    }
}
