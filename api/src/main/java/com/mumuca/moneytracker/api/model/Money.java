package com.mumuca.moneytracker.api.model;

import com.mumuca.moneytracker.api.account.exceptions.DifferentCurrenciesException;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Money {
    private BigDecimal amount;
    private String currency;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        return this.currency.equalsIgnoreCase(((Money) other).currency);
    }

    public void add(Money money) {
        if (!this.currency.equalsIgnoreCase(money.currency)) {
            throw new DifferentCurrenciesException("Cannot add money values of different currencies.");
        }

        this.amount = this.amount.add(money.amount);
    }

    public void add(BigDecimal amount) {
        this.amount = this.amount.add(amount);
    }

    public void subtract(Money money) {
        if (!this.currency.equalsIgnoreCase(money.currency)) {
            throw new DifferentCurrenciesException("Cannot subtract money values of different currencies.");
        }

        this.amount = this.amount.subtract(money.amount);
    }

    public void subtract(BigDecimal amount) {
        this.amount = this.amount.subtract(amount);
    }
}
