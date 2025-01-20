package com.mumuca.moneytracker.api.account.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EditTransferDTO(
        @Size(max = 30, message = "Title must not exceed 30 characters")
        String title,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        String currency,

        String fromAccount,

        String toAccount,

        LocalDate billingDate
) {
}
