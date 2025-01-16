package com.mumuca.moneytracker.api.account.dto;

import com.mumuca.moneytracker.api.account.model.AccountType;

import java.math.BigDecimal;

public record AccountDTO(
        String id,
        String name,
        String color,
        String icon,
        AccountType type,
        BigDecimal balance,
        String currency,
        boolean isArchived
) {}
