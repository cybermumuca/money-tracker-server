package com.mumuca.moneytracker.api.providers;

import com.mumuca.moneytracker.api.account.model.RecurrenceInterval;

import java.time.LocalDate;
import java.util.List;

public interface DateProvider {
    List<LocalDate> generateDates(LocalDate startDate, RecurrenceInterval frequency, int occurrences);
}
