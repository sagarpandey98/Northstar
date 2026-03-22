package com.sagarpandey.activity_tracker.utils;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

public class WeekUtils {

    private WeekUtils() {}

    /**
     * Returns the Monday of the week containing the given date.
     * Example: Wednesday 2025-08-27 → Monday 2025-08-25
     */
    public static LocalDate getMondayOf(LocalDate date) {
        if (date == null) return null;
        return date.with(TemporalAdjusters.previousOrSame(
            DayOfWeek.MONDAY
        ));
    }

    /**
     * Returns the Monday of the current week.
     */
    public static LocalDate getCurrentWeekMonday() {
        return getMondayOf(LocalDate.now());
    }

    /**
     * Returns the Monday of N weeks ago.
     * Example: weeksAgo(1) = last Monday
     */
    public static LocalDate weeksAgo(int weeks) {
        return getCurrentWeekMonday().minusWeeks(weeks);
    }

    /**
     * Returns true if the given date falls within the
     * current week (Monday to Sunday inclusive).
     */
    public static boolean isCurrentWeek(LocalDate date) {
        if (date == null) return false;
        LocalDate monday = getCurrentWeekMonday();
        LocalDate sunday = monday.plusDays(6);
        return !date.isBefore(monday) && !date.isAfter(sunday);
    }

    /**
     * Calculates consistency score for a week.
     * Returns null if targetFrequency is null or 0.
     * Caps at 100.
     */
    public static Double calculateConsistencyScore(
            Integer activitiesLogged,
            Integer targetFrequencyWeekly) {
        if (targetFrequencyWeekly == null 
                || targetFrequencyWeekly == 0) {
            return null;
        }
        if (activitiesLogged == null) return 0.0;
        double score = (activitiesLogged.doubleValue() 
            / targetFrequencyWeekly.doubleValue()) * 100.0;
        return Math.min(100.0, score);
    }
}
