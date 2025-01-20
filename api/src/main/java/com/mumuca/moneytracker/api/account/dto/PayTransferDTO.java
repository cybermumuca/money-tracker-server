package com.mumuca.moneytracker.api.account.dto;

import java.time.LocalDate;

public record PayTransferDTO(
        String accountId,
        LocalDate paidDate
) {}
