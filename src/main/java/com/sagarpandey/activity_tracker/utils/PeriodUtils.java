package com.sagarpandey.activity_tracker.utils;

import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class PeriodUtils {

    private PeriodUtils() {}

    /**
     * Returns the start date of the period containing
     * the given date for the specified period type.
     */
    public static LocalDate getPeriodStart(
            LocalDate date,
            EvaluationPeriod periodType,
            Integer customPeriodDays,
            LocalDate customPeriodAnchor) {

        if (date == null || periodType == null) return null;

        return switch (periodType) {
            case DAILY -> date;
            case WEEKLY -> date.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            case MONTHLY -> date.withDayOfMonth(1);
            case QUARTERLY -> {
                int month = date.getMonthValue();
                int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
                yield date.withMonth(quarterStartMonth)
                          .withDayOfMonth(1);
            }
            case YEARLY -> date.withDayOfYear(1);
            case CUSTOM -> {
                if (customPeriodDays == null
                        || customPeriodDays <= 0) {
                    yield date;
                }
                if (customPeriodAnchor == null) {
                    yield date;
                }
                // Find which custom period window date falls in
                long daysSinceAnchor = java.time.temporal
                    .ChronoUnit.DAYS
                    .between(customPeriodAnchor, date);
                if (daysSinceAnchor < 0) yield customPeriodAnchor;
                long periodsElapsed =
                    daysSinceAnchor / customPeriodDays;
                yield customPeriodAnchor.plusDays(
                    periodsElapsed * customPeriodDays
                );
            }
        };
    }

    /**
     * Returns the end date of the period (inclusive).
     */
    public static LocalDate getPeriodEnd(
            LocalDate periodStart,
            EvaluationPeriod periodType,
            Integer customPeriodDays) {

        if (periodStart == null || periodType == null)
            return null;

        return switch (periodType) {
            case DAILY -> periodStart;
            case WEEKLY -> periodStart.plusDays(6);
            case MONTHLY -> periodStart.with(
                TemporalAdjusters.lastDayOfMonth()
            );
            case QUARTERLY -> {
                // End of quarter = last day of 3rd month
                // from period start
                yield periodStart.plusMonths(2).with(
                    TemporalAdjusters.lastDayOfMonth()
                );
            }
            case YEARLY -> periodStart.with(
                TemporalAdjusters.lastDayOfYear()
            );
            case CUSTOM -> {
                int days = customPeriodDays != null
                    ? customPeriodDays : 1;
                yield periodStart.plusDays(days - 1);
            }
        };
    }

    /**
     * Returns the start of the PREVIOUS period.
     * Used for momentum/trend calculation.
     */
    public static LocalDate getPreviousPeriodStart(
            LocalDate currentPeriodStart,
            EvaluationPeriod periodType,
            Integer customPeriodDays) {

        if (currentPeriodStart == null || periodType == null)
            return null;

        return switch (periodType) {
            case DAILY -> currentPeriodStart.minusDays(1);
            case WEEKLY -> currentPeriodStart.minusWeeks(1);
            case MONTHLY -> currentPeriodStart.minusMonths(1);
            case QUARTERLY -> currentPeriodStart.minusMonths(3);
            case YEARLY -> currentPeriodStart.minusYears(1);
            case CUSTOM -> {
                int days = customPeriodDays != null
                    ? customPeriodDays : 1;
                yield currentPeriodStart.minusDays(days);
            }
        };
    }

    /**
     * Returns N previous period start dates for momentum.
     * Index 0 = oldest, index N-1 = most recent previous.
     */
    public static LocalDate[] getLastNPeriodStarts(
            LocalDate currentPeriodStart,
            EvaluationPeriod periodType,
            Integer customPeriodDays,
            int n) {

        LocalDate[] starts = new LocalDate[n];
        LocalDate cursor = currentPeriodStart;

        for (int i = n - 1; i >= 0; i--) {
            starts[i] = cursor;
            cursor = getPreviousPeriodStart(
                cursor, periodType, customPeriodDays
            );
        }
        return starts;
    }

    /**
     * Calculates consistency score for a period.
     * Returns null if target is null or 0.
     * Caps at 100.
     */
    public static Double calculatePeriodConsistencyScore(
            Integer activitiesLogged,
            Integer targetPerPeriod) {
        if (targetPerPeriod == null || targetPerPeriod == 0)
            return null;
        if (activitiesLogged == null) return 0.0;
        double score = (activitiesLogged.doubleValue()
            / targetPerPeriod.doubleValue()) * 100.0;
        return Math.min(100.0, score);
    }
}
