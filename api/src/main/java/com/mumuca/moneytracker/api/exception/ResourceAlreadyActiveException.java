package com.mumuca.moneytracker.api.exception;

public class ResourceAlreadyActiveException extends RuntimeException {
    public ResourceAlreadyActiveException(String message) {
        super(message);
    }
    public ResourceAlreadyActiveException() {
        super("Unable to unarchive a resource that is already active");
    }
}
