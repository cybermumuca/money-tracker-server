package com.mumuca.moneytracker.api.providers.impl;

import com.mumuca.moneytracker.api.account.model.RecurrenceInterval;
import com.mumuca.moneytracker.api.providers.DateProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class DateProviderImpl implements DateProvider {
    @Override
    public List<LocalDate> generateDates(
            LocalDate startDate,
            RecurrenceInterval frequency,
            int occurrences
    ) {
        return IntStream
                .range(0, occurrences)
                .mapToObj(i -> switch (frequency) {
                    case DAILY -> startDate.plusDays(i);
                    case WEEKLY -> startDate.plusWeeks(i);
                    case BIWEEKLY -> startDate.plusWeeks(i * 2L);
                    case MONTHLY -> startDate.plusMonths(i);
                    case BIMONTHLY -> startDate.plusMonths(i * 2L);
                    case TRIMONTHLY -> startDate.plusMonths(i * 3L);
                    case SIXMONTHLY -> startDate.plusMonths(i * 6L);
                    case YEARLY -> startDate.plusYears(i);
                })
                .collect(Collectors.toList());
    }
}
