package com.mumuca.moneytracker.api.providers.impl;

import com.mumuca.moneytracker.api.providers.CurrencyProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CurrencyProviderImpl implements CurrencyProvider {

    @Override
    public BigDecimal convertCurrency(
            BigDecimal amount,
            String fromCurrency,
            String toCurrency
    ) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
