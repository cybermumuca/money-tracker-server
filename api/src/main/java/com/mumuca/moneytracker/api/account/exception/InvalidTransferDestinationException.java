package com.mumuca.moneytracker.api.account.exception;

public class InvalidTransferDestinationException extends RuntimeException {
    public InvalidTransferDestinationException() {
        super("Impossible to pay transfer without a destination account.");
    }

    public InvalidTransferDestinationException(final String message) {
        super(message);
    }
}
