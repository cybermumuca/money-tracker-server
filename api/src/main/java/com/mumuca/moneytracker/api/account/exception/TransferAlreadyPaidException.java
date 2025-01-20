package com.mumuca.moneytracker.api.account.exception;

public class TransferAlreadyPaidException extends RuntimeException {
    public TransferAlreadyPaidException() {
        super("Transfer already paid.");
    }
}
