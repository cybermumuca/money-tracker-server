package com.mumuca.moneytracker.api.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferDTO(
        String id,
        String title,
        String description,
        AccountDTO fromAccount,
        AccountDTO toAccount,
        BigDecimal value,
        String currency,
        LocalDate billingDate,
        Boolean paid,
        String recurrenceId
) {}
