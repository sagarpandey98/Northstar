package com.sagarpandey.activity_tracker.utils;

import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility to evaluate ScheduleSpec rules against specific dates.
 * Implements Phase 4 of the Schedule Spec System.
 */
public class ScheduleSpecEvaluator {

    /**
     * Determines if a goal is actionable on a specific date based on its ScheduleSpec.
     */
    public static boolean isActionable(ScheduleSpec spec, LocalDate date) {
        if (spec == null) return true; // No spec means always actionable

        // 1. Check Exclusions (Veto rules)
        if (spec.getExclusions() != null) {
            for (ScheduleSpec.Exclusion exclusion : spec.getExclusions()) {
                if (isExcluded(exclusion, date)) return false;
            }
        }

        // 2. If no segments, then it's globally actionable (unless excluded)
        if (spec.getSegments() == null || spec.getSegments().isEmpty()) {
            return true;
        }

        // 3. Evaluate Hierarchical Segments
        if (spec.getSegments() != null && !spec.getSegments().isEmpty()) {
            boolean segmentMatch = matchesSegments(spec.getSegments(), date);
            if (segmentMatch) return true;
        }

        // 4. Fallback to Root Frequency
        // If it's a simple DAILY goal or a flexible WEEKLY goal without restrictive segments
        if (spec.getFrequency() != null) {
            String freq = spec.getFrequency().toUpperCase();
            if (freq.equals("DAILY")) return true;
            if (freq.equals("WEEKLY") && (spec.getFlexible() != null && spec.getFlexible())) {
                return true; // Flexible weekly is actionable every day until target met
            }
        }

        // If explicitly restricted by segments and didn't match, or no info found
        return (spec.getSegments() == null || spec.getSegments().isEmpty()) && spec.getFrequency() == null;
    }

    private static boolean isExcluded(ScheduleSpec.Exclusion exclusion, LocalDate date) {
        if (exclusion.getType() == null || exclusion.getValue() == null) return false;

        switch (exclusion.getType().toUpperCase()) {
            case "DATE":
                return date.toString().equals(exclusion.getValue());
            case "DAY_OF_WEEK":
                return date.getDayOfWeek().name().equalsIgnoreCase(exclusion.getValue());
            case "SPECIFIC_MONTH":
                return date.getMonth().name().equalsIgnoreCase(exclusion.getValue());
            default:
                return false;
        }
    }

    private static boolean matchesSegments(List<ScheduleSpec.Segment> segments, LocalDate date) {
        // If any segment at the current level matches, we proceed to nested segments (if any)
        // This is an OR relationship between segments at the same level generally, 
        // but often there's just one segment per level in the UI.
        
        for (ScheduleSpec.Segment segment : segments) {
            if (matchesSegmentCriteria(segment, date)) {
                
                // If this is a leaf segment (no children), or if it matches and has no sub-segments,
                // then we've satisfied this branch.
                if (segment.getSegments() == null || segment.getSegments().isEmpty()) {
                    return true; 
                }
                
                // If it has children, the children must also match (recursive AND)
                if (matchesSegments(segment.getSegments(), date)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private static boolean matchesSegmentCriteria(ScheduleSpec.Segment segment, LocalDate date) {
        if (segment.getFrequency() == null) return true;
        if (segment.getFlexible() != null && segment.getFlexible()) return true;

        List<String> values = segment.getValues();
        if (values == null || values.isEmpty()) return true;

        switch (segment.getFrequency().toUpperCase()) {
            case "YEARLY":
                return values.contains(date.getMonth().name());
            case "MONTHLY":
                // Standard UI sends W1, W2 etc or D1, D2 etc.
                // We'll check if any match.
                return matchesMonthlyCriteria(values, date);
            case "WEEKLY":
                return values.stream().anyMatch(v -> v.equalsIgnoreCase(date.getDayOfWeek().name()));
            case "DAILY":
                // If parent was weekly, values are likely days. If parent was monthly, values are likely dates.
                String dayName = date.getDayOfWeek().name();
                String dayOfMonth = "D" + date.getDayOfMonth();
                return values.stream().anyMatch(v -> v.equalsIgnoreCase(dayName) || v.equalsIgnoreCase(dayOfMonth));
            case "TIMING":
                // Timing is tricky for a date-only check. 
                // Usually we assume if it's there, it's actionable on that day at some point.
                return true; 
            default:
                return true;
        }
    }

    private static boolean matchesMonthlyCriteria(List<String> values, LocalDate date) {
        for (String val : values) {
            if (val.startsWith("W")) {
                int weekOfMonth = (date.getDayOfMonth() - 1) / 7 + 1;
                if (val.equals("W" + weekOfMonth)) return true;
            }
            if (val.startsWith("D")) {
                if (val.equals("D" + date.getDayOfMonth())) return true;
            }
        }
        return false;
    }

    /**
     * Counts how many days between start and end (inclusive) are actionable for the given spec.
     */
    public static int countActionableDays(LocalDate start, LocalDate end, ScheduleSpec spec) {
        if (spec == null) return (int) (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
        
        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (isActionable(spec, current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}
