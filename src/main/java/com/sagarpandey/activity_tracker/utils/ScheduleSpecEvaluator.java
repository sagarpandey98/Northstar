package com.sagarpandey.activity_tracker.utils;

import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates schedule_spec V2 rule trees.
 */
public class ScheduleSpecEvaluator {

    private static final int SUPPORTED_VERSION = 2;

    /**
     * Date-only actionability check. Time rules are treated as planned slots on
     * the matching date, so they do not make the date itself non-actionable.
     */
    public static boolean isActionable(ScheduleSpec spec, LocalDate date) {
        if (date == null) {
            return false;
        }
        if (spec == null) {
            return true;
        }
        if (isExcluded(spec, date)) {
            return false;
        }
        if (spec.getRules() == null || spec.getRules().isEmpty()) {
            return true;
        }

        return spec.getRules().stream().anyMatch(rule -> matchesRule(spec, rule, date, null));
    }

    /**
     * Date-time actionability check. The supplied LocalDateTime is interpreted in
     * the schedule's timezone.
     */
    public static boolean isActionable(ScheduleSpec spec, LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        if (spec == null) {
            return true;
        }
        LocalDate date = dateTime.toLocalDate();
        if (isExcluded(spec, date)) {
            return false;
        }
        if (spec.getRules() == null || spec.getRules().isEmpty()) {
            return true;
        }

        return spec.getRules().stream()
            .anyMatch(rule -> matchesRule(spec, rule, date, dateTime.toLocalTime()));
    }

    public static int countActionableDays(LocalDate start, LocalDate end, ScheduleSpec spec) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        if (spec == null) {
            return (int) ChronoUnit.DAYS.between(start, end) + 1;
        }

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

    public static List<LocalDate> listActionableDates(ScheduleSpec spec, LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        if (start == null || end == null || end.isBefore(start)) {
            return dates;
        }

        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (isActionable(spec, current)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    public static List<String> validate(ScheduleSpec spec) {
        List<String> errors = new ArrayList<>();
        if (spec == null) {
            return errors;
        }

        if (spec.getVersion() == null || spec.getVersion() != SUPPORTED_VERSION) {
            errors.add("scheduleSpec.version must be " + SUPPORTED_VERSION);
        }
        if (spec.getScheduleType() == null) {
            errors.add("scheduleSpec.scheduleType is required");
        }
        if (spec.getTimezone() == null || spec.getTimezone().isBlank()) {
            errors.add("scheduleSpec.timezone is required");
        } else {
            try {
                ZoneId.of(spec.getTimezone());
            } catch (Exception e) {
                errors.add("scheduleSpec.timezone must be a valid IANA timezone");
            }
        }

        validateRequirements(spec.getRequirements(), "scheduleSpec.requirements", errors);

        int rootRank = rootRank(spec.getScheduleType());
        validateRules(spec.getRules(), rootRank, "scheduleSpec.rules", errors);
        validateExclusions(spec.getExclusions(), errors);
        validateWeekStartsOn(spec.getWeekStartsOn(), errors);

        return errors;
    }

    private static boolean matchesRule(
            ScheduleSpec spec,
            ScheduleSpec.Rule rule,
            LocalDate date,
            LocalTime time) {
        if (rule == null || rule.getScope() == null || rule.getMode() == null) {
            return false;
        }
        if (!matchesScope(spec, rule, date, time)) {
            return false;
        }
        if (rule.getMode() == ScheduleSpec.RuleMode.FLEXIBLE) {
            return true;
        }
        if (rule.getRules() != null && !rule.getRules().isEmpty()) {
            return rule.getRules().stream().anyMatch(child -> matchesRule(spec, child, date, time));
        }
        return true;
    }

    private static boolean matchesScope(
            ScheduleSpec spec,
            ScheduleSpec.Rule rule,
            LocalDate date,
            LocalTime time) {
        return switch (rule.getScope()) {
            case QUARTER -> containsInt(rule.getValues(), quarterOfYear(date));
            case MONTH_OF_YEAR -> containsInt(rule.getValues(), date.getMonthValue());
            case MONTH_OF_QUARTER -> containsInt(rule.getValues(), monthOfQuarter(date));
            case WEEK_OF_MONTH -> containsInt(rule.getValues(), weekOfMonth(date, spec));
            case DAY_OF_MONTH -> containsDayOfMonth(rule.getValues(), date);
            case DAY_OF_WEEK -> containsName(rule.getValues(), date.getDayOfWeek().name());
            case TIME_OF_DAY -> time == null || containsTime(rule.getValues(), time);
            case TIME_WINDOW -> time == null || matchesTimeWindow(rule.getWindows(), time);
        };
    }

    private static boolean isExcluded(ScheduleSpec spec, LocalDate date) {
        if (spec.getExclusions() == null || spec.getExclusions().isEmpty()) {
            return false;
        }

        for (ScheduleSpec.Exclusion exclusion : spec.getExclusions()) {
            if (matchesExclusion(exclusion, date)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesExclusion(ScheduleSpec.Exclusion exclusion, LocalDate date) {
        if (exclusion == null || exclusion.getType() == null) {
            return false;
        }

        try {
            return switch (exclusion.getType()) {
                case DATE -> exclusion.getValue() != null
                    && date.equals(LocalDate.parse(exclusion.getValue().toString()));
                case DATE_RANGE -> exclusion.getStart() != null
                    && exclusion.getEnd() != null
                    && !date.isBefore(LocalDate.parse(exclusion.getStart()))
                    && !date.isAfter(LocalDate.parse(exclusion.getEnd()));
                case DAY_OF_WEEK -> exclusion.getValue() != null
                    && date.getDayOfWeek().name().equalsIgnoreCase(exclusion.getValue().toString());
                case MONTH_OF_YEAR -> exclusion.getValue() != null
                    && date.getMonthValue() == parseInt(exclusion.getValue(), -1);
            };
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static int quarterOfYear(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    private static int monthOfQuarter(LocalDate date) {
        return (date.getMonthValue() - 1) % 3 + 1;
    }

    private static int weekOfMonth(LocalDate date, ScheduleSpec spec) {
        if (spec != null && spec.getWeekOfMonthModel() == ScheduleSpec.WeekOfMonthModel.CALENDAR_WEEKS) {
            LocalDate monthStart = date.withDayOfMonth(1);
            DayOfWeek weekStart = resolveWeekStartsOn(spec.getWeekStartsOn());
            int offset = Math.floorMod(monthStart.getDayOfWeek().getValue() - weekStart.getValue(), 7);
            return (date.getDayOfMonth() + offset - 1) / 7 + 1;
        }
        return (date.getDayOfMonth() - 1) / 7 + 1;
    }

    private static boolean containsDayOfMonth(List<Object> values, LocalDate date) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = value.toString();
            if ("LAST".equalsIgnoreCase(text) && date.getDayOfMonth() == date.lengthOfMonth()) {
                return true;
            }
            if (parseInt(value, -1) == date.getDayOfMonth()) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsInt(List<Object> values, int expected) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.stream().anyMatch(value -> parseInt(value, Integer.MIN_VALUE) == expected);
    }

    private static boolean containsName(List<Object> values, String expected) {
        if (values == null || values.isEmpty() || expected == null) {
            return false;
        }
        return values.stream()
            .anyMatch(value -> value != null && expected.equalsIgnoreCase(value.toString()));
    }

    private static boolean containsTime(List<Object> values, LocalTime time) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            try {
                if (LocalTime.parse(value.toString()).equals(time.truncatedTo(ChronoUnit.MINUTES))) {
                    return true;
                }
            } catch (DateTimeParseException ignored) {
                // Invalid values are rejected by validation and ignored by evaluation.
            }
        }
        return false;
    }

    private static boolean matchesTimeWindow(List<ScheduleSpec.TimeWindow> windows, LocalTime time) {
        if (windows == null || windows.isEmpty()) {
            return false;
        }
        LocalTime normalized = time.truncatedTo(ChronoUnit.MINUTES);
        for (ScheduleSpec.TimeWindow window : windows) {
            if (window == null || window.getStart() == null || window.getEnd() == null) {
                continue;
            }
            try {
                LocalTime start = LocalTime.parse(window.getStart());
                LocalTime end = LocalTime.parse(window.getEnd());
                if (isWithinWindow(normalized, start, end)) {
                    return true;
                }
            } catch (DateTimeParseException ignored) {
                // Invalid values are rejected by validation and ignored by evaluation.
            }
        }
        return false;
    }

    private static boolean isWithinWindow(LocalTime time, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return time.equals(start);
        }
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void validateRules(
            List<ScheduleSpec.Rule> rules,
            int parentRank,
            String path,
            List<String> errors) {
        if (rules == null) {
            return;
        }

        for (int i = 0; i < rules.size(); i++) {
            ScheduleSpec.Rule rule = rules.get(i);
            String rulePath = path + "[" + i + "]";
            if (rule == null) {
                errors.add(rulePath + " must not be null");
                continue;
            }

            if (rule.getScope() == null) {
                errors.add(rulePath + ".scope is required");
                continue;
            }
            if (rule.getMode() == null) {
                errors.add(rulePath + ".mode is required");
            }

            int rank = scopeRank(rule.getScope());
            if (rank <= parentRank) {
                errors.add(rulePath + ".scope must move from broader to narrower time units");
            }

            boolean hasChildren = rule.getRules() != null && !rule.getRules().isEmpty();
            if (rule.getMode() == ScheduleSpec.RuleMode.FLEXIBLE && hasChildren) {
                errors.add(rulePath + " cannot have child rules when mode is FLEXIBLE");
            }

            validateScopeValues(rule, rulePath, errors);
            validateRequirements(rule.getRequirements(), rulePath + ".requirements", errors);
            validateRules(rule.getRules(), rank, rulePath + ".rules", errors);
        }
    }

    private static void validateScopeValues(ScheduleSpec.Rule rule, String path, List<String> errors) {
        if (rule.getScope() == ScheduleSpec.RuleScope.TIME_WINDOW) {
            validateTimeWindows(rule.getWindows(), path + ".windows", errors);
            return;
        }

        if (rule.getValues() == null || rule.getValues().isEmpty()) {
            errors.add(path + ".values is required for " + rule.getScope());
            return;
        }

        for (Object value : rule.getValues()) {
            if (!isValidValueForScope(rule.getScope(), value)) {
                errors.add(path + ".values contains invalid value '" + value + "' for " + rule.getScope());
            }
        }
    }

    private static boolean isValidValueForScope(ScheduleSpec.RuleScope scope, Object value) {
        if (value == null) {
            return false;
        }
        return switch (scope) {
            case QUARTER -> between(value, 1, 4);
            case MONTH_OF_YEAR -> between(value, 1, 12);
            case MONTH_OF_QUARTER -> between(value, 1, 3);
            case WEEK_OF_MONTH -> between(value, 1, 5);
            case DAY_OF_MONTH -> "LAST".equalsIgnoreCase(value.toString()) || between(value, 1, 31);
            case DAY_OF_WEEK -> isValidDayOfWeek(value.toString());
            case TIME_OF_DAY -> isValidTime(value.toString());
            case TIME_WINDOW -> false;
        };
    }

    private static boolean between(Object value, int min, int max) {
        int parsed = parseInt(value, Integer.MIN_VALUE);
        return parsed >= min && parsed <= max;
    }

    private static boolean isValidDayOfWeek(String value) {
        try {
            DayOfWeek.valueOf(value.toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidTime(String value) {
        try {
            LocalTime.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static void validateTimeWindows(
            List<ScheduleSpec.TimeWindow> windows,
            String path,
            List<String> errors) {
        if (windows == null || windows.isEmpty()) {
            errors.add(path + " is required for TIME_WINDOW");
            return;
        }

        for (int i = 0; i < windows.size(); i++) {
            ScheduleSpec.TimeWindow window = windows.get(i);
            String windowPath = path + "[" + i + "]";
            if (window == null || window.getStart() == null || window.getEnd() == null) {
                errors.add(windowPath + " requires start and end");
                continue;
            }
            if (!isValidTime(window.getStart())) {
                errors.add(windowPath + ".start must be valid HH:mm");
            }
            if (!isValidTime(window.getEnd())) {
                errors.add(windowPath + ".end must be valid HH:mm");
            }
        }
    }

    private static void validateRequirements(
            ScheduleSpec.Requirements requirements,
            String path,
            List<String> errors) {
        if (requirements == null) {
            return;
        }

        Integer min = requirements.getMinCheckins();
        Integer max = requirements.getMaxCheckins();
        if (min != null && min < 0) {
            errors.add(path + ".minCheckins must be non-negative");
        }
        if (max != null && max < 0) {
            errors.add(path + ".maxCheckins must be non-negative");
        }
        if (min != null && max != null && max < min) {
            errors.add(path + ".maxCheckins must be greater than or equal to minCheckins");
        }
    }

    private static void validateExclusions(
            List<ScheduleSpec.Exclusion> exclusions,
            List<String> errors) {
        if (exclusions == null) {
            return;
        }

        for (int i = 0; i < exclusions.size(); i++) {
            ScheduleSpec.Exclusion exclusion = exclusions.get(i);
            String path = "scheduleSpec.exclusions[" + i + "]";
            if (exclusion == null || exclusion.getType() == null) {
                errors.add(path + ".type is required");
                continue;
            }

            switch (exclusion.getType()) {
                case DATE -> {
                    if (exclusion.getValue() == null || !isValidDate(exclusion.getValue().toString())) {
                        errors.add(path + ".value must be a valid YYYY-MM-DD date");
                    }
                }
                case DATE_RANGE -> {
                    if (exclusion.getStart() == null || !isValidDate(exclusion.getStart())) {
                        errors.add(path + ".start must be a valid YYYY-MM-DD date");
                    }
                    if (exclusion.getEnd() == null || !isValidDate(exclusion.getEnd())) {
                        errors.add(path + ".end must be a valid YYYY-MM-DD date");
                    }
                }
                case DAY_OF_WEEK -> {
                    if (exclusion.getValue() == null || !isValidDayOfWeek(exclusion.getValue().toString())) {
                        errors.add(path + ".value must be a valid day of week");
                    }
                }
                case MONTH_OF_YEAR -> {
                    if (exclusion.getValue() == null || !between(exclusion.getValue(), 1, 12)) {
                        errors.add(path + ".value must be between 1 and 12");
                    }
                }
            }
        }
    }

    private static boolean isValidDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static void validateWeekStartsOn(String weekStartsOn, List<String> errors) {
        if (weekStartsOn == null || weekStartsOn.isBlank()) {
            return;
        }
        try {
            DayOfWeek.valueOf(weekStartsOn.toUpperCase());
        } catch (Exception e) {
            errors.add("scheduleSpec.weekStartsOn must be a valid day of week");
        }
    }

    private static DayOfWeek resolveWeekStartsOn(String weekStartsOn) {
        if (weekStartsOn == null || weekStartsOn.isBlank()) {
            return DayOfWeek.MONDAY;
        }
        try {
            return DayOfWeek.valueOf(weekStartsOn.toUpperCase());
        } catch (Exception e) {
            return DayOfWeek.MONDAY;
        }
    }

    private static int rootRank(ScheduleSpec.ScheduleType scheduleType) {
        if (scheduleType == null) {
            return 0;
        }
        return switch (scheduleType) {
            case YEARLY -> 0;
            case QUARTERLY -> 1;
            case MONTHLY -> 2;
            case WEEKLY -> 3;
            case DAILY -> 4;
        };
    }

    private static int scopeRank(ScheduleSpec.RuleScope scope) {
        return switch (scope) {
            case QUARTER -> 1;
            case MONTH_OF_YEAR, MONTH_OF_QUARTER -> 2;
            case WEEK_OF_MONTH -> 3;
            case DAY_OF_MONTH, DAY_OF_WEEK -> 4;
            case TIME_OF_DAY, TIME_WINDOW -> 5;
        };
    }
}
