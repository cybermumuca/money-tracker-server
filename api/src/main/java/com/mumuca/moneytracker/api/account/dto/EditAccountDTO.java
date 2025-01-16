package com.mumuca.moneytracker.api.account.dto;

import com.mumuca.moneytracker.api.account.model.AccountType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EditAccountDTO(
        AccountType type,

        @Size(max = 30, message = "Name must not exceed 30 characters")
        String name,

        @PositiveOrZero(message = "Balance must be zero or positive")
        BigDecimal balance,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        String currency,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Color must be a valid hex code")
        String color,

        @Size(max = 50, message = "Icon name must not exceed 50 characters")
        String icon
) {}
