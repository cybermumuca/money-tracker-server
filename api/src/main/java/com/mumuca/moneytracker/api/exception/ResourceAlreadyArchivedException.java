package com.mumuca.moneytracker.api.exception;

public class ResourceAlreadyArchivedException extends RuntimeException {
    public ResourceAlreadyArchivedException(String message) {
        super(message);
    }

    public ResourceAlreadyArchivedException() {
        super("Unable to archive a resource that is already archived");
    }
}
