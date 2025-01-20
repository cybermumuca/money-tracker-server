package com.mumuca.moneytracker.api.account.dto;

import com.mumuca.moneytracker.api.account.model.RecurrenceInterval;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterRepeatedTransferDTO(
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

        LocalDate paidDate,

        @NotNull(message = "Recurrence is required")
        RecurrenceInterval recurrenceInterval,

        @NotNull(message = "Number of recurrences is required")
        @Max(value = 200, message = "Number of recurrences must be less than or equal to 200")
        @Positive(message = "Number of recurrences must be positive")
        Integer numberOfRecurrences
) {}
