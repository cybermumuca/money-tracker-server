package com.mumuca.moneytracker.api.account.exception;

public class TransferNotPaidYetException extends RuntimeException {
    public TransferNotPaidYetException() {
        super("Unable to unpay an unpaid transfer.");
    }
}
