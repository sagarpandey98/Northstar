package com.sagarpandey.activity_tracker.utils;

import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleSpecEvaluatorTest {

    @Test
    void monthlyFixedDatesMatchOnlySelectedDays() {
        ScheduleSpec spec = baseSpec(ScheduleSpec.ScheduleType.MONTHLY);
        spec.setRules(List.of(rule(
            ScheduleSpec.RuleScope.DAY_OF_MONTH,
            ScheduleSpec.RuleMode.STRICT,
            10, 25, 28
        )));

        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 5, 10)));
        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 5, 25)));
        assertFalse(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 5, 11)));
    }

    @Test
    void monthlyWeekDrillDownSupportsStrictAndFlexibleWeeks() {
        ScheduleSpec spec = baseSpec(ScheduleSpec.ScheduleType.MONTHLY);
        spec.setRules(List.of(
            ruleWithChildren(
                ScheduleSpec.RuleScope.WEEK_OF_MONTH,
                ScheduleSpec.RuleMode.STRICT,
                values(1),
                List.of(rule(
                    ScheduleSpec.RuleScope.DAY_OF_WEEK,
                    ScheduleSpec.RuleMode.STRICT,
                    "MONDAY", "WEDNESDAY", "FRIDAY"
                ))
            ),
            rule(ScheduleSpec.RuleScope.WEEK_OF_MONTH, ScheduleSpec.RuleMode.FLEXIBLE, 3)
        ));

        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 1, 5)));
        assertFalse(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 1, 6)));
        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 1, 16)));
        assertFalse(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 1, 23)));
    }

    @Test
    void yearlyDeepDrillDownSupportsQuarterMonthWeekDayAndTime() {
        ScheduleSpec spec = baseSpec(ScheduleSpec.ScheduleType.YEARLY);
        spec.setRules(List.of(
            ruleWithChildren(
                ScheduleSpec.RuleScope.QUARTER,
                ScheduleSpec.RuleMode.STRICT,
                values(1),
                List.of(
                    ruleWithChildren(
                        ScheduleSpec.RuleScope.MONTH_OF_QUARTER,
                        ScheduleSpec.RuleMode.STRICT,
                        values(1),
                        List.of(
                            ruleWithChildren(
                                ScheduleSpec.RuleScope.WEEK_OF_MONTH,
                                ScheduleSpec.RuleMode.STRICT,
                                values(1),
                                List.of(rule(
                                    ScheduleSpec.RuleScope.DAY_OF_WEEK,
                                    ScheduleSpec.RuleMode.STRICT,
                                    "MONDAY", "WEDNESDAY", "FRIDAY"
                                ))
                            ),
                            rule(ScheduleSpec.RuleScope.WEEK_OF_MONTH, ScheduleSpec.RuleMode.FLEXIBLE, 2),
                            ruleWithChildren(
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
                            )
                        )
                    ),
                    rule(ScheduleSpec.RuleScope.MONTH_OF_QUARTER, ScheduleSpec.RuleMode.FLEXIBLE, 3)
                )
            ),
            rule(ScheduleSpec.RuleScope.QUARTER, ScheduleSpec.RuleMode.FLEXIBLE, 4)
        ));

        assertTrue(ScheduleSpecEvaluator.validate(spec).isEmpty());
        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 1, 5)));
        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 1, 10)));
        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDateTime.of(2026, 1, 19, 9, 0)));
        assertFalse(ScheduleSpecEvaluator.isActionable(spec, LocalDateTime.of(2026, 1, 19, 10, 0)));
        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 3, 12)));
        assertFalse(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 4, 12)));
        assertTrue(ScheduleSpecEvaluator.isActionable(spec, LocalDate.of(2026, 10, 12)));
    }

    @Test
    void validationRejectsFlexibleRulesWithChildren() {
        ScheduleSpec spec = baseSpec(ScheduleSpec.ScheduleType.MONTHLY);
        spec.setRules(List.of(ruleWithChildren(
            ScheduleSpec.RuleScope.WEEK_OF_MONTH,
            ScheduleSpec.RuleMode.FLEXIBLE,
            values(1),
            List.of(rule(ScheduleSpec.RuleScope.DAY_OF_WEEK, ScheduleSpec.RuleMode.STRICT, "MONDAY"))
        )));

        assertFalse(ScheduleSpecEvaluator.validate(spec).isEmpty());
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
}
