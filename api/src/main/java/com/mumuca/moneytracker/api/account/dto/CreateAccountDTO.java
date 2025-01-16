package com.mumuca.moneytracker.api.account.dto;

import com.mumuca.moneytracker.api.account.model.AccountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountDTO(
        @NotNull(message = "Account type is required")
        AccountType type,

        @NotBlank(message = "Name is required")
        @Size(max = 30, message = "Name must not exceed 30 characters")
        String name,

        @PositiveOrZero(message = "Balance must be zero or positive")
        BigDecimal balance,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        String currency,

        @NotBlank(message = "Color is required")
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Color must be a valid hex code")
        String color,

        @NotBlank(message = "Icon is required")
        @Size(max = 50, message = "Icon name must not exceed 50 characters")
        String icon
) {}
