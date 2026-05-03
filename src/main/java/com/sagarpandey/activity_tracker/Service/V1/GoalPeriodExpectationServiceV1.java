package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodExpectationService;
import com.sagarpandey.activity_tracker.dtos.health.GoalDayExpectation;
import com.sagarpandey.activity_tracker.dtos.health.GoalPeriodExpectation;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import com.sagarpandey.activity_tracker.utils.ScheduleSpecEvaluator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GoalPeriodExpectationServiceV1 implements GoalPeriodExpectationService {

    @Override
    public GoalPeriodExpectation buildExpectation(Goal goal, GoalPeriod period) {
        ScheduleSpec spec = resolveScheduleSpec(goal, period);
        LocalDate today = LocalDate.now(resolveZoneId(spec));
        return buildExpectation(goal, period, today);
    }

    @Override
    public GoalPeriodExpectation buildExpectation(Goal goal, GoalPeriod period, LocalDate evaluationDate) {
        GoalPeriodExpectation expectation = new GoalPeriodExpectation();
        if (goal == null || period == null || period.getPeriodStart() == null || period.getPeriodEnd() == null) {
            return expectation;
        }

        ScheduleSpec spec = resolveScheduleSpec(goal, period);
        List<LocalDate> periodDates = enumerateDates(period.getPeriodStart(), period.getPeriodEnd());
        Map<LocalDate, Double> expectedMinimumByDate = zeroMap(periodDates);
        Map<LocalDate, Double> expectedTargetByDate = zeroMap(periodDates);

        distributeUnits(
            spec,
            periodDates,
            scheduleRules(spec),
            sanitize(goal.getMinimumSessionPeriod()),
            expectedMinimumByDate
        );
        distributeUnits(
            spec,
            periodDates,
            scheduleRules(spec),
            sanitize(goal.getMaximumSessionPeriod()),
            expectedTargetByDate
        );

        LocalDate effectiveEvaluationDate = evaluationDate;
        List<GoalDayExpectation> dailyExpectations = new ArrayList<>();
        int actionableDayCount = 0;
        int actionableDayCountToDate = 0;
        double totalExpectedMinimumUnits = 0.0;
        double totalExpectedTargetUnits = 0.0;
        double expectedMinimumUnitsToDate = 0.0;
        double expectedTargetUnitsToDate = 0.0;

        for (LocalDate date : periodDates) {
            boolean actionable = isActionable(spec, date);
            double expectedMinimumUnits = expectedMinimumByDate.getOrDefault(date, 0.0);
            double expectedTargetUnits = expectedTargetByDate.getOrDefault(date, 0.0);

            if (actionable) {
                actionableDayCount++;
            }

            totalExpectedMinimumUnits += expectedMinimumUnits;
            totalExpectedTargetUnits += expectedTargetUnits;

            boolean countedToDate = effectiveEvaluationDate != null && !date.isAfter(effectiveEvaluationDate);
            if (countedToDate) {
                if (actionable) {
                    actionableDayCountToDate++;
                }
                expectedMinimumUnitsToDate += expectedMinimumUnits;
                expectedTargetUnitsToDate += expectedTargetUnits;
            }

            dailyExpectations.add(new GoalDayExpectation(
                date,
                actionable,
                round(expectedMinimumUnits),
                round(expectedTargetUnits)
            ));
        }

        expectation.setGoalUuid(goal.getUuid());
        expectation.setPeriodUuid(period.getUuid());
        expectation.setPeriodStart(period.getPeriodStart());
        expectation.setPeriodEnd(period.getPeriodEnd());
        expectation.setEvaluationDate(effectiveEvaluationDate);
        expectation.setActionableDayCount(actionableDayCount);
        expectation.setActionableDayCountToDate(actionableDayCountToDate);
        expectation.setTotalExpectedMinimumUnits(round(totalExpectedMinimumUnits));
        expectation.setTotalExpectedTargetUnits(round(totalExpectedTargetUnits));
        expectation.setExpectedMinimumUnitsToDate(round(expectedMinimumUnitsToDate));
        expectation.setExpectedTargetUnitsToDate(round(expectedTargetUnitsToDate));
        expectation.setDailyExpectations(dailyExpectations);
        return expectation;
    }

    private void distributeUnits(
            ScheduleSpec spec,
            List<LocalDate> candidateDates,
            List<ScheduleSpec.Rule> rules,
            double totalUnits,
            Map<LocalDate, Double> allocation) {
        if (totalUnits <= 0.0 || candidateDates == null || candidateDates.isEmpty()) {
            return;
        }

        List<LocalDate> activeDates = candidateDates.stream()
            .filter(date -> isActionable(spec, date))
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());
        if (activeDates.isEmpty()) {
            return;
        }

        if (rules == null || rules.isEmpty()) {
            allocateUniform(activeDates, totalUnits, allocation);
            return;
        }

        List<RuleBranch> branches = new ArrayList<>();
        for (ScheduleSpec.Rule rule : rules) {
            branches.addAll(expandRule(spec, activeDates, rule));
        }

        if (branches.isEmpty()) {
            return;
        }

        double branchShare = totalUnits / branches.size();
        for (RuleBranch branch : branches) {
            if (branch.dates.isEmpty()) {
                continue;
            }
            if (branch.mode == ScheduleSpec.RuleMode.FLEXIBLE || branch.children.isEmpty()) {
                allocateUniform(branch.dates, branchShare, allocation);
            } else {
                distributeUnits(spec, branch.dates, branch.children, branchShare, allocation);
            }
        }
    }

    private List<RuleBranch> expandRule(
            ScheduleSpec spec,
            List<LocalDate> candidateDates,
            ScheduleSpec.Rule rule) {
        List<RuleBranch> branches = new ArrayList<>();
        if (rule == null || rule.getScope() == null || rule.getMode() == null || candidateDates.isEmpty()) {
            return branches;
        }

        List<ValueSelector> selectors = selectorsForRule(spec, rule);
        if (selectors.isEmpty()) {
            selectors = List.of(new ValueSelector(date -> true));
        }

        for (ValueSelector selector : selectors) {
            List<LocalDate> matchedDates = candidateDates.stream()
                .filter(selector::matches)
                .filter(date -> isActionable(spec, date))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
            if (matchedDates.isEmpty()) {
                continue;
            }
            branches.add(new RuleBranch(
                matchedDates,
                rule.getMode(),
                rule.getRules() != null ? rule.getRules() : List.of()
            ));
        }
        return branches;
    }

    private List<ValueSelector> selectorsForRule(ScheduleSpec spec, ScheduleSpec.Rule rule) {
        List<ValueSelector> selectors = new ArrayList<>();
        if (rule == null || rule.getScope() == null) {
            return selectors;
        }

        if (rule.getScope() == ScheduleSpec.RuleScope.TIME_WINDOW) {
            if (rule.getWindows() == null || rule.getWindows().isEmpty()) {
                return selectors;
            }
            for (int i = 0; i < rule.getWindows().size(); i++) {
                selectors.add(new ValueSelector(date -> true));
            }
            return selectors;
        }

        List<Object> values = rule.getValues();
        if (values == null || values.isEmpty()) {
            return selectors;
        }

        for (Object value : values) {
            selectors.add(selectorForScope(spec, rule.getScope(), value));
        }
        return selectors.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private ValueSelector selectorForScope(ScheduleSpec spec, ScheduleSpec.RuleScope scope, Object rawValue) {
        return switch (scope) {
            case QUARTER -> new ValueSelector(date -> quarterOfYear(date) == parseInt(rawValue, Integer.MIN_VALUE));
            case MONTH_OF_YEAR -> new ValueSelector(date -> date.getMonthValue() == parseInt(rawValue, Integer.MIN_VALUE));
            case MONTH_OF_QUARTER -> new ValueSelector(date -> monthOfQuarter(date) == parseInt(rawValue, Integer.MIN_VALUE));
            case WEEK_OF_MONTH -> new ValueSelector(date -> weekOfMonth(date, spec) == parseInt(rawValue, Integer.MIN_VALUE));
            case DAY_OF_MONTH -> new ValueSelector(date -> matchesDayOfMonth(date, rawValue));
            case DAY_OF_WEEK -> new ValueSelector(date -> matchesDayOfWeek(date, rawValue));
            case TIME_OF_DAY, TIME_WINDOW -> new ValueSelector(date -> true);
        };
    }

    private void allocateUniform(
            List<LocalDate> dates,
            double units,
            Map<LocalDate, Double> allocation) {
        if (dates == null || dates.isEmpty() || units <= 0.0) {
            return;
        }
        double perDate = units / dates.size();
        for (LocalDate date : dates) {
            allocation.merge(date, perDate, Double::sum);
        }
    }

    private ScheduleSpec resolveScheduleSpec(Goal goal, GoalPeriod period) {
        if (period != null && period.getScheduleSpec() != null) {
            return period.getScheduleSpec();
        }
        return goal != null ? goal.getScheduleSpec() : null;
    }

    private ZoneId resolveZoneId(ScheduleSpec spec) {
        if (spec != null && spec.getTimezone() != null && !spec.getTimezone().isBlank()) {
            try {
                return ZoneId.of(spec.getTimezone());
            } catch (Exception ignored) {
                // Fall through to UTC.
            }
        }
        return ZoneId.of("UTC");
    }

    private List<ScheduleSpec.Rule> scheduleRules(ScheduleSpec spec) {
        if (spec == null || spec.getRules() == null) {
            return List.of();
        }
        return spec.getRules();
    }

    private boolean isActionable(ScheduleSpec spec, LocalDate date) {
        return spec == null || ScheduleSpecEvaluator.isActionable(spec, date);
    }

    private List<LocalDate> enumerateDates(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        if (start == null || end == null || end.isBefore(start)) {
            return dates;
        }
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private Map<LocalDate, Double> zeroMap(List<LocalDate> dates) {
        Map<LocalDate, Double> values = new LinkedHashMap<>();
        for (LocalDate date : dates) {
            values.put(date, 0.0);
        }
        return values;
    }

    private int quarterOfYear(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    private int monthOfQuarter(LocalDate date) {
        return (date.getMonthValue() - 1) % 3 + 1;
    }

    private int weekOfMonth(LocalDate date, ScheduleSpec spec) {
        if (spec != null && spec.getWeekOfMonthModel() == ScheduleSpec.WeekOfMonthModel.CALENDAR_WEEKS) {
            LocalDate monthStart = date.withDayOfMonth(1);
            DayOfWeek weekStartsOn = resolveWeekStartsOn(spec.getWeekStartsOn());
            int offset = Math.floorMod(monthStart.getDayOfWeek().getValue() - weekStartsOn.getValue(), 7);
            return (date.getDayOfMonth() + offset - 1) / 7 + 1;
        }
        return (date.getDayOfMonth() - 1) / 7 + 1;
    }

    private DayOfWeek resolveWeekStartsOn(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DayOfWeek.MONDAY;
        }
        try {
            return DayOfWeek.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return DayOfWeek.MONDAY;
        }
    }

    private boolean matchesDayOfMonth(LocalDate date, Object rawValue) {
        if (rawValue == null) {
            return false;
        }
        String value = rawValue.toString();
        if ("LAST".equalsIgnoreCase(value)) {
            return date.getDayOfMonth() == date.lengthOfMonth();
        }
        return date.getDayOfMonth() == parseInt(rawValue, Integer.MIN_VALUE);
    }

    private boolean matchesDayOfWeek(LocalDate date, Object rawValue) {
        if (rawValue == null) {
            return false;
        }
        try {
            DayOfWeek expected = DayOfWeek.valueOf(rawValue.toString().toUpperCase(Locale.ROOT));
            return date.getDayOfWeek() == expected;
        } catch (Exception ex) {
            return false;
        }
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double sanitize(Number value) {
        if (value == null) {
            return 0.0;
        }
        double numeric = value.doubleValue();
        return numeric > 0.0 ? numeric : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class RuleBranch {
        private final List<LocalDate> dates;
        private final ScheduleSpec.RuleMode mode;
        private final List<ScheduleSpec.Rule> children;

        private RuleBranch(
                List<LocalDate> dates,
                ScheduleSpec.RuleMode mode,
                List<ScheduleSpec.Rule> children) {
            this.dates = dates;
            this.mode = mode;
            this.children = children;
        }
    }

    @FunctionalInterface
    private interface LocalDateMatcher {
        boolean matches(LocalDate date);
    }

    private static class ValueSelector {
        private final LocalDateMatcher matcher;

        private ValueSelector(LocalDateMatcher matcher) {
            this.matcher = matcher;
        }

        private boolean matches(LocalDate date) {
            return matcher.matches(date);
        }
    }
}
