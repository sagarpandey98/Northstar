package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.dtos.health.GoalDayExpectation;
import com.sagarpandey.activity_tracker.dtos.health.GoalPeriodExpectation;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalPeriodExpectationServiceV1Test {

    private final GoalPeriodExpectationServiceV1 service = new GoalPeriodExpectationServiceV1();

    @Test
    void weeklyFlexibleDistributesExpectationAcrossEveryDay() {
        Goal goal = baseGoal(7, 14, baseSpec(ScheduleSpec.ScheduleType.WEEKLY));
        GoalPeriod period = period("weekly-period", LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10), goal);

        GoalPeriodExpectation expectation = service.buildExpectation(goal, period, period.getPeriodEnd());
        Map<LocalDate, GoalDayExpectation> byDate = byDate(expectation);

        assertEquals(7.0, expectation.getTotalExpectedMinimumUnits(), 0.001);
        assertEquals(14.0, expectation.getTotalExpectedTargetUnits(), 0.001);
        assertEquals(1.0, byDate.get(LocalDate.of(2026, 5, 4)).getExpectedMinimumUnits(), 0.001);
        assertEquals(2.0, byDate.get(LocalDate.of(2026, 5, 4)).getExpectedTargetUnits(), 0.001);
        assertEquals(1.0, byDate.get(LocalDate.of(2026, 5, 10)).getExpectedMinimumUnits(), 0.001);
        assertEquals(2.0, byDate.get(LocalDate.of(2026, 5, 10)).getExpectedTargetUnits(), 0.001);
    }

    @Test
    void monthlySpecificAndFlexibleWeeksDistributeHierarchically() {
        ScheduleSpec spec = baseSpec(ScheduleSpec.ScheduleType.MONTHLY);
        spec.setRules(List.of(
            ruleWithChildren(
                ScheduleSpec.RuleScope.WEEK_OF_MONTH,
                ScheduleSpec.RuleMode.STRICT,
                values(1),
                List.of(rule(
                    ScheduleSpec.RuleScope.DAY_OF_WEEK,
                    ScheduleSpec.RuleMode.STRICT,
                    "MONDAY", "FRIDAY"
                ))
            ),
            rule(ScheduleSpec.RuleScope.WEEK_OF_MONTH, ScheduleSpec.RuleMode.FLEXIBLE, 3)
        ));

        Goal goal = baseGoal(6, 12, spec);
        GoalPeriod period = period("monthly-period", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), goal);

        GoalPeriodExpectation expectation = service.buildExpectation(goal, period, period.getPeriodEnd());
        Map<LocalDate, GoalDayExpectation> byDate = byDate(expectation);

        assertEquals(6.0, expectation.getTotalExpectedMinimumUnits(), 0.001);
        assertEquals(12.0, expectation.getTotalExpectedTargetUnits(), 0.001);
        assertEquals(1.5, byDate.get(LocalDate.of(2026, 1, 2)).getExpectedMinimumUnits(), 0.001);
        assertEquals(3.0, byDate.get(LocalDate.of(2026, 1, 2)).getExpectedTargetUnits(), 0.001);
        assertEquals(1.5, byDate.get(LocalDate.of(2026, 1, 5)).getExpectedMinimumUnits(), 0.001);
        assertEquals(0.43, byDate.get(LocalDate.of(2026, 1, 16)).getExpectedMinimumUnits(), 0.001);
        assertEquals(0.86, byDate.get(LocalDate.of(2026, 1, 16)).getExpectedTargetUnits(), 0.001);
        assertEquals(0.0, byDate.get(LocalDate.of(2026, 1, 8)).getExpectedMinimumUnits(), 0.001);
    }

    @Test
    void yearlyNestedRulesAllocateIntoSelectedQuarterMonthWeekDayAndTimes() {
        ScheduleSpec spec = baseSpec(ScheduleSpec.ScheduleType.YEARLY);
        spec.setRules(List.of(
            ruleWithChildren(
                ScheduleSpec.RuleScope.QUARTER,
                ScheduleSpec.RuleMode.STRICT,
                values(1),
                List.of(ruleWithChildren(
                    ScheduleSpec.RuleScope.MONTH_OF_QUARTER,
                    ScheduleSpec.RuleMode.STRICT,
                    values(1),
                    List.of(ruleWithChildren(
                        ScheduleSpec.RuleScope.WEEK_OF_MONTH,
                        ScheduleSpec.RuleMode.STRICT,
                        values(3),
                        List.of(ruleWithChildren(
                            ScheduleSpec.RuleScope.DAY_OF_WEEK,
                            ScheduleSpec.RuleMode.STRICT,
                            values("MONDAY"),
                            List.of(rule(
                                ScheduleSpec.RuleScope.TIME_OF_DAY,
                                ScheduleSpec.RuleMode.STRICT,
                                "09:00", "21:00"
                            ))
                        ))
                    ))
                ))
            ),
            rule(ScheduleSpec.RuleScope.QUARTER, ScheduleSpec.RuleMode.FLEXIBLE, 4)
        ));

        Goal goal = baseGoal(12, 24, spec);
        GoalPeriod period = period("yearly-period", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), goal);

        GoalPeriodExpectation expectation = service.buildExpectation(goal, period, period.getPeriodEnd());
        Map<LocalDate, GoalDayExpectation> byDate = byDate(expectation);

        assertEquals(12.0, expectation.getTotalExpectedMinimumUnits(), 0.001);
        assertEquals(24.0, expectation.getTotalExpectedTargetUnits(), 0.001);
        assertEquals(6.0, byDate.get(LocalDate.of(2026, 1, 19)).getExpectedMinimumUnits(), 0.001);
        assertEquals(12.0, byDate.get(LocalDate.of(2026, 1, 19)).getExpectedTargetUnits(), 0.001);
        assertEquals(0.07, byDate.get(LocalDate.of(2026, 10, 12)).getExpectedMinimumUnits(), 0.001);
        assertEquals(0.13, byDate.get(LocalDate.of(2026, 10, 12)).getExpectedTargetUnits(), 0.001);
        assertEquals(0.0, byDate.get(LocalDate.of(2026, 4, 12)).getExpectedMinimumUnits(), 0.001);
    }

    private Goal baseGoal(int minimumSessionPeriod, int maximumSessionPeriod, ScheduleSpec scheduleSpec) {
        Goal goal = new Goal();
        goal.setUuid("goal-uuid");
        goal.setMinimumSessionPeriod(minimumSessionPeriod);
        goal.setMaximumSessionPeriod(maximumSessionPeriod);
        goal.setScheduleSpec(scheduleSpec);
        return goal;
    }

    private GoalPeriod period(String uuid, LocalDate start, LocalDate end, Goal goal) {
        GoalPeriod period = new GoalPeriod();
        period.setUuid(uuid);
        period.setParentGoalUuid(goal.getUuid());
        period.setGoal(goal);
        period.setPeriodStart(start);
        period.setPeriodEnd(end);
        period.setScheduleSpec(goal.getScheduleSpec());
        return period;
    }

    private ScheduleSpec baseSpec(ScheduleSpec.ScheduleType scheduleType) {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setVersion(2);
        spec.setScheduleType(scheduleType);
        spec.setTimezone("Asia/Kolkata");
        return spec;
    }

    private ScheduleSpec.Rule rule(
            ScheduleSpec.RuleScope scope,
            ScheduleSpec.RuleMode mode,
            Object... values) {
        ScheduleSpec.Rule rule = new ScheduleSpec.Rule();
        rule.setScope(scope);
        rule.setMode(mode);
        rule.setValues(values(values));
        return rule;
    }

    private ScheduleSpec.Rule ruleWithChildren(
            ScheduleSpec.RuleScope scope,
            ScheduleSpec.RuleMode mode,
            List<Object> values,
            List<ScheduleSpec.Rule> children) {
        ScheduleSpec.Rule rule = new ScheduleSpec.Rule();
        rule.setScope(scope);
        rule.setMode(mode);
        rule.setValues(values);
        rule.setRules(children);
        return rule;
    }

    private List<Object> values(Object... values) {
        return Arrays.asList(values);
    }

    private Map<LocalDate, GoalDayExpectation> byDate(GoalPeriodExpectation expectation) {
        return expectation.getDailyExpectations().stream()
            .collect(Collectors.toMap(GoalDayExpectation::getDate, Function.identity()));
    }
}
