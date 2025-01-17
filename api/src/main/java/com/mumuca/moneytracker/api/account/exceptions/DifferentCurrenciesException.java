package com.mumuca.moneytracker.api.account.exceptions;

public class DifferentCurrenciesException extends RuntimeException {
    public DifferentCurrenciesException(String message) {
        super(message);
    }

    public DifferentCurrenciesException() { super("Currencies must be the same."); }
}
