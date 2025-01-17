package com.mumuca.moneytracker.api.providers;

import java.math.BigDecimal;

public interface CurrencyProvider {
    BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency);
}
