package com.mumuca.moneytracker.api.account.dto;

import com.mumuca.moneytracker.api.account.model.RecurrenceInterval;
import com.mumuca.moneytracker.api.account.model.RecurrenceType;
import com.mumuca.moneytracker.api.account.model.TransactionType;

import java.time.LocalDate;
import java.util.List;

public record RecurrenceDTO<T>(
        String id,
        RecurrenceInterval interval,
        LocalDate firstOccurrence,
        TransactionType transactionType,
        RecurrenceType recurrenceType,
        List<T> recurrences
) {}
