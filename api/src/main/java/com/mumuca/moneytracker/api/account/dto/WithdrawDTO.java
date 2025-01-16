package com.mumuca.moneytracker.api.account.dto;

import java.math.BigDecimal;

public record WithdrawDTO(
        BigDecimal amount
) {}
