package com.mumuca.moneytracker.api.account.exception.handler;

import com.mumuca.moneytracker.api.account.controller.TransferController;
import com.mumuca.moneytracker.api.account.exception.InvalidTransferDestinationException;
import com.mumuca.moneytracker.api.account.exception.InvalidTransferSourceException;
import com.mumuca.moneytracker.api.account.exception.TransferAlreadyPaidException;
import com.mumuca.moneytracker.api.account.exception.TransferNotPaidYetException;
import com.mumuca.moneytracker.api.exception.dto.APIErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.mumuca.moneytracker.api.util.HttpUtils.buildErrorResponse;

@RestControllerAdvice(assignableTypes = {TransferController.class})
public class TransferControllerExceptionHandler {
    @ExceptionHandler(InvalidTransferDestinationException.class)
    public ResponseEntity<APIErrorResponse<String>> handleInvalidTransferDestinationException(final InvalidTransferDestinationException ex) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid Transfer Destination",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidTransferSourceException.class)
    public ResponseEntity<APIErrorResponse<String>> handleInvalidTransferSourceException(final InvalidTransferSourceException ex) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid Transfer Source",
                ex.getMessage()
        );
    }

    @ExceptionHandler(TransferAlreadyPaidException.class)
    public ResponseEntity<APIErrorResponse<String>> handleTransferAlreadyPaidException(final TransferAlreadyPaidException ex) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Transfer Already Paid",
                ex.getMessage()
        );
    }

    @ExceptionHandler(TransferNotPaidYetException.class)
    public ResponseEntity<APIErrorResponse<String>> handleTransferNotPaidYetException(final TransferNotPaidYetException ex) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Transfer Not Paid Yet",
                ex.getMessage()
        );
    }
}
