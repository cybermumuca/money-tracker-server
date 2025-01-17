package com.mumuca.moneytracker.api.providers.impl;

import com.mumuca.moneytracker.api.account.model.RecurrenceInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = DateProviderImplTest.Config.class)
@DisplayName("DateProviderImpl Tests")
class DateProviderImplTest {

    @ComponentScan(basePackageClasses = DateProviderImpl.class)
    static class Config {}

    @Autowired
    private DateProviderImpl sut;

    @Nested
    @DisplayName("generateDates tests")
    class GenerateDatesTests {
        @Test
        @DisplayName("should be able to generate daily dates")
        void shouldBeAbleToGenerateDailyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.DAILY, 5);

            // Assert
            assertThat(result).hasSize(5)
                    .containsExactly(
                            startDate,
                            startDate.plusDays(1),
                            startDate.plusDays(2),
                            startDate.plusDays(3),
                            startDate.plusDays(4)
                    );
        }

        @Test
        @DisplayName("should be able to generate weekly dates")
        void shouldBeAbleToGenerateWeeklyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.WEEKLY, 3);

            // Assert
            assertThat(result).hasSize(3)
                    .containsExactly(
                            startDate,
                            startDate.plusWeeks(1),
                            startDate.plusWeeks(2)
                    );
        }

        @Test
        @DisplayName("should be able to generate biweekly dates")
        void shouldBeAbleToGenerateBiweeklyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.BIWEEKLY, 3);

            // Assert
            assertThat(result).hasSize(3)
                    .containsExactly(
                            startDate,
                            startDate.plusWeeks(2),
                            startDate.plusWeeks(4)
                    );
        }

        @Test
        @DisplayName("should be able to generate monthly dates")
        void shouldBeAbleToGenerateMonthlyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.MONTHLY, 4);

            // Assert
            assertThat(result).hasSize(4)
                    .containsExactly(
                            startDate,
                            startDate.plusMonths(1),
                            startDate.plusMonths(2),
                            startDate.plusMonths(3)
                    );
        }

        @Test
        @DisplayName("should be able to generate bimonthly dates")
        void shouldBeAbleToGenerateBimonthlyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.BIMONTHLY, 4);

            // Assert
            assertThat(result).hasSize(4)
                    .containsExactly(
                            startDate,
                            startDate.plusMonths(2),
                            startDate.plusMonths(4),
                            startDate.plusMonths(6)
                    );
        }

        @Test
        @DisplayName("should be able to generate trimonthly dates")
        void shouldBeAbleToGenerateTrimonthlyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.TRIMONTHLY, 4);

            // Assert
            assertThat(result).hasSize(4)
                    .containsExactly(
                            startDate,
                            startDate.plusMonths(3),
                            startDate.plusMonths(6),
                            startDate.plusMonths(9)
                    );
        }

        @Test
        @DisplayName("should be able to generate sixmonthly dates")
        void shouldBeAbleToGenerateSixmonthlyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.SIXMONTHLY, 4);

            // Assert
            assertThat(result).hasSize(4)
                    .containsExactly(
                            startDate,
                            startDate.plusMonths(6),
                            startDate.plusMonths(12),
                            startDate.plusMonths(18)
                    );
        }

        @Test
        @DisplayName("should be able to generate yearly dates")
        void shouldBeAbleToGenerateYearlyDates() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.YEARLY, 4);

            // Assert
            assertThat(result).hasSize(4)
                    .containsExactly(
                            startDate,
                            startDate.plusYears(1),
                            startDate.plusYears(2),
                            startDate.plusYears(3)
                    );
        }

        @Test
        @DisplayName("should return empty list when occurrences is zero")
        void shouldReturnEmptyListWhenOccurrencesIsZero() {
            // Arrange
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // Act
            List<LocalDate> result = sut.generateDates(startDate, RecurrenceInterval.DAILY, 0);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}