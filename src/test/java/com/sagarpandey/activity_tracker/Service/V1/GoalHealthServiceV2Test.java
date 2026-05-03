package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.ActivityRepository;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodExpectationService;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.Activity;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalHealthServiceV2Test {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalPeriodRepository goalPeriodRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Spy
    private GoalPeriodExpectationService expectationService = new GoalPeriodExpectationServiceV1();

    @InjectMocks
    private GoalHealthServiceV2 goalHealthService;

    @Test
    void specificSchedulesDoNotLetBunchedActivityFakeConsistency() {
        Goal goal = baseGoal("goal-1", Goal.Priority.HIGH, 3, 3, weeklySpecificSpec("MONDAY", "WEDNESDAY", "FRIDAY"));
        GoalPeriod period = period("period-1", LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 3), goal);

        when(goalPeriodRepository.findByParentGoalUuid(goal.getUuid())).thenReturn(List.of(period));
        when(activityRepository.findGoalActivitiesOverlappingPeriod(eq(goal.getId()), eq(goal.getUserId()), any(), any()))
            .thenReturn(List.of(
                countActivity(goal.getId(), "2026-04-27T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-27T12:00:00Z"),
                countActivity(goal.getId(), "2026-04-27T18:00:00Z")
            ));

        assertEquals(33.33, goalHealthService.calculatePeriodConsistencyScore(period), 0.01);
        assertEquals(33.33, goalHealthService.calculatePeriodProgressScore(period), 0.01);
        assertEquals(100.0, goalHealthService.calculatePeriodMomentumScore(period), 0.01);
    }

    @Test
    void momentumComparesAgainstAverageOfPreviousTwoPeriods() {
        Goal goal = baseGoal("goal-2", Goal.Priority.HIGH, 7, 7, flexibleSpec(ScheduleSpec.ScheduleType.WEEKLY));
        GoalPeriod first = period("period-1", LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 12), goal);
        GoalPeriod second = period("period-2", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 19), goal);
        GoalPeriod third = period("period-3", LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26), goal);

        when(goalPeriodRepository.findByParentGoalUuid(goal.getUuid())).thenReturn(List.of(first, second, third));
        when(activityRepository.findGoalActivitiesOverlappingPeriod(eq(goal.getId()), eq(goal.getUserId()), any(), any()))
            .thenReturn(Arrays.asList(
                countActivity(goal.getId(), "2026-04-06T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-07T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-08T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-09T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-10T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-11T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-12T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-13T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-14T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-15T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-16T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-20T09:00:00Z"),
                countActivity(goal.getId(), "2026-04-21T09:00:00Z")
            ));

        assertEquals(36.36, goalHealthService.calculatePeriodMomentumScore(third), 0.01);
    }

    @Test
    void parentGoalHealthRollsUpFromChildrenUsingPriorityWeights() {
        Goal parent = baseGoal("parent-goal", Goal.Priority.MEDIUM, 0, 0, flexibleSpec(ScheduleSpec.ScheduleType.MONTHLY));
        parent.setUserId("user-1");

        Goal criticalChild = baseGoal("critical-child", Goal.Priority.CRITICAL, 0, 0, flexibleSpec(ScheduleSpec.ScheduleType.MONTHLY));
        criticalChild.setHealthScore(100.0);
        criticalChild.setConsistencyScore(90.0);
        criticalChild.setMomentumScore(80.0);
        criticalChild.setProgressScore(70.0);

        Goal lowChild = baseGoal("low-child", Goal.Priority.LOW, 0, 0, flexibleSpec(ScheduleSpec.ScheduleType.MONTHLY));
        lowChild.setHealthScore(0.0);
        lowChild.setConsistencyScore(30.0);
        lowChild.setMomentumScore(20.0);
        lowChild.setProgressScore(10.0);

        when(goalRepository.findByParentGoalIdAndUserIdAndIsDeletedFalse(parent.getUuid(), parent.getUserId()))
            .thenReturn(List.of(criticalChild, lowChild));

        assertEquals(80.0, goalHealthService.calculateOverallHealthScore(parent), 0.01);
        assertEquals(78.0, goalHealthService.calculateConsistencyScore(parent), 0.01);
        assertEquals(68.0, goalHealthService.calculateMomentumScore(parent), 0.01);
        assertEquals(58.0, goalHealthService.calculateProgressScore(parent), 0.01);
    }

    private Goal baseGoal(String uuid, Goal.Priority priority, int minimumSessionPeriod, int maximumSessionPeriod, ScheduleSpec scheduleSpec) {
        Goal goal = new Goal();
        goal.setId(Math.abs(uuid.hashCode()) + 1L);
        goal.setUuid(uuid);
        goal.setUserId("user-1");
        goal.setPriority(priority);
        goal.setMetric(Goal.Metric.COUNT);
        goal.setMinimumSessionPeriod(minimumSessionPeriod);
        goal.setMaximumSessionPeriod(maximumSessionPeriod);
        goal.setScheduleSpec(scheduleSpec);
        goal.setConsistencyWeight(40);
        goal.setMomentumWeight(20);
        goal.setProgressWeight(40);
        goal.setHealthStatus(HealthStatus.UNTRACKED);
        return goal;
    }

    private GoalPeriod period(String uuid, LocalDate start, LocalDate end, Goal goal) {
        GoalPeriod period = new GoalPeriod();
        period.setUuid(uuid);
        period.setGoal(goal);
        period.setParentGoalUuid(goal.getUuid());
        period.setPeriodStart(start);
        period.setPeriodEnd(end);
        period.setScheduleSpec(goal.getScheduleSpec());
        return period;
    }

    private ScheduleSpec flexibleSpec(ScheduleSpec.ScheduleType scheduleType) {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setVersion(2);
        spec.setScheduleType(scheduleType);
        spec.setTimezone("UTC");
        return spec;
    }

    private ScheduleSpec weeklySpecificSpec(String... days) {
        ScheduleSpec spec = flexibleSpec(ScheduleSpec.ScheduleType.WEEKLY);
        ScheduleSpec.Rule rule = new ScheduleSpec.Rule();
        rule.setScope(ScheduleSpec.RuleScope.DAY_OF_WEEK);
        rule.setMode(ScheduleSpec.RuleMode.STRICT);
        rule.setValues(Arrays.asList(days));
        spec.setRules(List.of(rule));
        return spec;
    }

    private Activity countActivity(Long goalId, String startTimeUtc) {
        Activity activity = new Activity();
        activity.setGoalId(goalId);
        OffsetDateTime start = OffsetDateTime.parse(startTimeUtc);
        activity.setStartTime(start);
        activity.setEndTime(start.plusMinutes(30));
        activity.setUserId("user-1");
        return activity;
    }
}
