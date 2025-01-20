package com.mumuca.moneytracker.api.account.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterUniqueTransferDTO(
        @NotBlank(message = "Title is required")
        @Size(max = 30, message = "Title must not exceed 30 characters")
        String title,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        String currency,

        @NotBlank(message = "Source account is required")
        String fromAccount,

        @NotBlank(message = "Destination account is required")
        String toAccount,

        @NotNull(message = "Billing date is required")
        LocalDate billingDate,

        LocalDate paidDate
) {}
