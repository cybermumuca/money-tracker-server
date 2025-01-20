package com.mumuca.moneytracker.api.account.exception;

public class InvalidTransferSourceException extends RuntimeException {
   public InvalidTransferSourceException() {
        super("Impossible to pay transfer without source account.");
   }
}
