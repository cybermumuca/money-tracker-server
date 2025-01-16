package com.mumuca.moneytracker.api.auth.exception;

public class CredentialsMismatchException extends RuntimeException {
    public CredentialsMismatchException() {
        super("Email or password is invalid.");
    }
}
