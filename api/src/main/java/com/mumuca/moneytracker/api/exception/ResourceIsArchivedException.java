package com.mumuca.moneytracker.api.exception;

public class ResourceIsArchivedException extends RuntimeException {
    public ResourceIsArchivedException(String message) {
        super(message);
    }

    public ResourceIsArchivedException() { super("Resource is archived"); }
}
