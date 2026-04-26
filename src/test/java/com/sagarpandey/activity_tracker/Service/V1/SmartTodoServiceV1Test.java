package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Service.Interface.ActivityServiceInterface;
import com.sagarpandey.activity_tracker.Service.Interface.GoalService;
import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.GoalStatsResponse;
import com.sagarpandey.activity_tracker.dtos.SmartTodoListResponse;
import com.sagarpandey.activity_tracker.dtos.SmartTodoResponse;
import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.models.Activity;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartTodoServiceV1Test {

    private static final String USER_ID = "user-123";

    @Test
    void getSmartTodosForDate_buildsBucketedListAndSortedFocusItems() {
        LocalDate targetDate = LocalDate.of(2026, 4, 24);

        GoalResponse mustDoGoal = goal(1L, "must-goal", "Morning Run", Goal.Priority.CRITICAL, weeklyStrict("FRIDAY", 3));
        GoalResponse catchUpGoal = goal(2L, "catchup-goal", "Spanish Practice", Goal.Priority.HIGH, flexibleWeekly(5));
        GoalResponse completedGoal = goal(3L, "done-goal", "Reading", Goal.Priority.MEDIUM, flexibleWeekly(1));
        catchUpGoal.setCurrentStreak(0);
        completedGoal.setCurrentStreak(0);

        GoalPeriod mustDoPeriod = period("must-goal", targetDate.minusDays(4), targetDate.plusDays(2));
        GoalPeriod catchUpPeriod = period("catchup-goal", targetDate.minusDays(4), targetDate.plusDays(2));
        GoalPeriod completedPeriod = period("done-goal", targetDate.minusDays(4), targetDate.plusDays(2));

        ActivityResponse catchUpActivity = activity(2L, "2026-04-21T07:00:00+05:30");
        ActivityResponse completedActivity = activity(3L, "2026-04-24T08:00:00+05:30");

        SmartTodoServiceV1 service = service(
            List.of(mustDoGoal, catchUpGoal, completedGoal),
            List.of(catchUpActivity, completedActivity),
            List.of(mustDoPeriod, catchUpPeriod, completedPeriod)
        );

        SmartTodoListResponse result = service.getSmartTodosForDate(USER_ID, targetDate);

        assertNotNull(result);
        assertEquals(3, result.getItems().size());
        assertEquals(1, result.getSummary().getMustDoTodayCount());
        assertEquals(1, result.getSummary().getCatchUpTodayCount());
        assertEquals(1, result.getSummary().getCompletedTodayCount());

        SmartTodoResponse first = result.getItems().get(0);
        SmartTodoResponse second = result.getItems().get(1);
        SmartTodoResponse third = result.getItems().get(2);

        assertEquals("Morning Run", first.getTitle());
        assertEquals("MUST_DO_TODAY", first.getTodoStatus());
        assertTrue(first.getReasonCodes().contains("SCHEDULED_TODAY"));
        assertTrue(first.isRecommendedFocus());

        assertEquals("Spanish Practice", second.getTitle());
        assertEquals("CATCH_UP_TODAY", second.getTodoStatus());
        assertTrue(second.getReasonCodes().contains("BEHIND_PACE"));
        assertTrue(second.isRecommendedFocus());

        assertEquals("Reading", third.getTitle());
        assertEquals("COMPLETED_TODAY", third.getTodoStatus());
        assertFalse(third.isRecommendedFocus());
        assertTrue(third.isCompletedToday());
    }

    @Test
    void getSmartTodosForDate_partialProgressDoesNotMarkGoalComplete() {
        LocalDate targetDate = LocalDate.of(2026, 4, 26);

        GoalResponse goal = goal(10L, "partial-goal", "Deep Work", Goal.Priority.HIGH, flexibleWeekly(4));
        GoalPeriod period = period("partial-goal", targetDate.minusDays(6), targetDate);
        ActivityResponse oneCheckin = activity(10L, "2026-04-26T09:00:00+05:30");

        SmartTodoServiceV1 service = service(List.of(goal), List.of(oneCheckin), List.of(period));
        SmartTodoListResponse result = service.getSmartTodosForDate(USER_ID, targetDate);

        assertEquals(1, result.getItems().size());
        SmartTodoResponse todo = result.getItems().get(0);
        assertFalse(todo.isCompletedToday());
        assertEquals(1, todo.getCurrentProgress());
        assertTrue(todo.getTargetProgress() > todo.getCurrentProgress());
        assertTrue(todo.getRemainingTodayTarget() > 0);
    }

    @Test
    void getSmartTodosForDate_explicitScheduleNotTodayAndOnTrackIsHidden() {
        LocalDate targetDate = LocalDate.of(2026, 4, 21);

        GoalResponse goal = goal(20L, "quiet-goal", "Weekly Review", Goal.Priority.MEDIUM, weeklyStrict("MONDAY", 1));
        GoalPeriod period = period("quiet-goal", LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26));
        ActivityResponse mondayLog = activity(20L, "2026-04-20T18:00:00+05:30");

        SmartTodoServiceV1 service = service(List.of(goal), List.of(mondayLog), List.of(period));
        SmartTodoListResponse result = service.getSmartTodosForDate(USER_ID, targetDate);

        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getSummary().getTotalItems());
    }

    @Test
    void getSmartTodosForDate_dailyGoalIsTreatedAsDueToday() {
        LocalDate targetDate = LocalDate.of(2026, 4, 22);

        GoalResponse dailyGoal = goal(30L, "daily-goal", "Meditation", Goal.Priority.MEDIUM, dailySchedule());
        GoalPeriod period = period("daily-goal", targetDate, targetDate);

        SmartTodoServiceV1 service = service(List.of(dailyGoal), List.of(), List.of(period));
        SmartTodoListResponse result = service.getSmartTodosForDate(USER_ID, targetDate);

        assertEquals(1, result.getItems().size());
        SmartTodoResponse todo = result.getItems().get(0);
        assertTrue(todo.isScheduledForToday());
        assertEquals("MUST_DO_TODAY", todo.getTodoStatus());
        assertTrue(todo.getReasonCodes().contains("SCHEDULED_TODAY"));
    }

    @Test
    void getSmartTodosForDate_weeklyFlexibleGoalStillAppearsWhenCatchUpIsNeeded() {
        LocalDate targetDate = LocalDate.of(2026, 4, 24);

        GoalResponse flexibleGoal = goal(31L, "flex-weekly", "Strength Training", Goal.Priority.HIGH, flexibleWeekly(4));
        flexibleGoal.setCurrentStreak(0);
        GoalPeriod period = period("flex-weekly", LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26));

        ActivityResponse mondayActivity = activity(31L, "2026-04-21T07:00:00+05:30");
        SmartTodoServiceV1 service = service(List.of(flexibleGoal), List.of(mondayActivity), List.of(period));
        SmartTodoListResponse result = service.getSmartTodosForDate(USER_ID, targetDate);

        assertEquals(1, result.getItems().size());
        SmartTodoResponse todo = result.getItems().get(0);
        assertEquals("CATCH_UP_TODAY", todo.getTodoStatus());
        assertFalse(todo.getReasonCodes().contains("SCHEDULED_TODAY"));
    }

    @Test
    void getSmartTodosForDate_monthlyQuarterlyAndYearlyRulesOnlySurfaceOnMatchingDates() {
        LocalDate matchingDate = LocalDate.of(2026, 1, 5);
        LocalDate nonMatchingDate = LocalDate.of(2026, 1, 6);

        GoalResponse monthlyGoal = goal(40L, "monthly-goal", "Monthly Review", Goal.Priority.MEDIUM, monthlyFixedDates(5, 25));
        GoalResponse quarterlyGoal = goal(41L, "quarterly-goal", "Quarter Plan", Goal.Priority.MEDIUM, quarterlyMonthOfQuarter(1));
        GoalResponse yearlyGoal = goal(42L, "yearly-goal", "Annual Deep Work", Goal.Priority.HIGH, yearlyDeepRule());

        List<GoalPeriod> periods = List.of(
            period("monthly-goal", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
            period("quarterly-goal", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
            period("yearly-goal", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        SmartTodoServiceV1 service = service(List.of(monthlyGoal, quarterlyGoal, yearlyGoal), List.of(), periods);

        SmartTodoListResponse matchingResult = service.getSmartTodosForDate(USER_ID, matchingDate);
        assertEquals(3, matchingResult.getItems().size());
        assertTrue(matchingResult.getItems().stream().allMatch(SmartTodoResponse::isScheduledForToday));

        SmartTodoListResponse nonMatchingResult = service.getSmartTodosForDate(USER_ID, nonMatchingDate);
        assertTrue(nonMatchingResult.getItems().stream().noneMatch(todo -> "Monthly Review".equals(todo.getTitle())));
        assertTrue(nonMatchingResult.getItems().stream().noneMatch(todo -> "Annual Deep Work".equals(todo.getTitle())));
        assertEquals(1, nonMatchingResult.getItems().size());
        assertEquals("Quarter Plan", nonMatchingResult.getItems().get(0).getTitle());
    }

    @Test
    void getSmartTodosForDate_usesMinimumTimeCommittedPeriodForTodaySuggestion() {
        LocalDate targetDate = LocalDate.of(2026, 4, 24);

        GoalResponse durationGoal = goal(50L, "duration-goal", "Focused Study", Goal.Priority.HIGH, flexibleWeekly(1));
        durationGoal.setMetric(Goal.Metric.DURATION);
        durationGoal.setMinimumTimeCommittedPeriod(210);
        durationGoal.setMinimumTimeCommittedDaily(20);

        GoalPeriod period = period("duration-goal", LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26));
        period.setMetric(Goal.Metric.DURATION);

        ActivityResponse monday = activity(50L, "2026-04-21T07:00:00+05:30");
        monday.setEndTime(monday.getStartTime().plusMinutes(60));

        SmartTodoServiceV1 service = service(List.of(durationGoal), List.of(monday), List.of(period));
        SmartTodoListResponse result = service.getSmartTodosForDate(USER_ID, targetDate);

        assertEquals(1, result.getItems().size());
        SmartTodoResponse todo = result.getItems().get(0);
        assertEquals(210, todo.getMinimumSessionPeriod());
        assertEquals(30, todo.getMinimumSessionDaily());
        assertEquals(90, todo.getSuggestedTimeMinutes());
        assertEquals(90, todo.getTargetProgress());
    }

    private SmartTodoServiceV1 service(
            List<GoalResponse> goals,
            List<ActivityResponse> activities,
            List<GoalPeriod> periods) {
        return new SmartTodoServiceV1(
            new StubGoalService(goals),
            new StubActivityService(activities),
            stubGoalPeriodRepository(periods)
        );
    }

    private GoalResponse goal(Long id, String uuid, String title, Goal.Priority priority, ScheduleSpec scheduleSpec) {
        GoalResponse goal = new GoalResponse();
        goal.setId(id);
        goal.setUuid(uuid);
        goal.setUserId(USER_ID);
        goal.setTitle(title);
        goal.setPriority(priority);
        goal.setStatus(Goal.Status.IN_PROGRESS);
        goal.setMetric(Goal.Metric.COUNT);
        goal.setGoalType(GoalType.HABIT);
        goal.setIsLeaf(true);
        goal.setIsTracked(true);
        goal.setCurrentStreak(2);
        goal.setScheduleSpec(scheduleSpec);
        goal.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        goal.setLastUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        return goal;
    }

    private GoalPeriod period(String goalUuid, LocalDate start, LocalDate end) {
        GoalPeriod period = new GoalPeriod();
        period.setParentGoalUuid(goalUuid);
        period.setMetric(Goal.Metric.COUNT);
        period.setTargetOperator(Goal.TargetOperator.GREATER_THAN);
        period.setPeriodStart(start);
        period.setPeriodEnd(end);
        period.setCurrentValue(0.0);
        return period;
    }

    private ActivityResponse activity(Long goalId, String startTime) {
        ActivityResponse activity = new ActivityResponse();
        activity.setGoalId(goalId);
        activity.setStartTime(OffsetDateTime.parse(startTime));
        activity.setEndTime(OffsetDateTime.parse(startTime).plusMinutes(30));
        return activity;
    }

    private ScheduleSpec weeklyStrict(String dayOfWeek, int minCheckins) {
        ScheduleSpec spec = baseWeekly(minCheckins);
        ScheduleSpec.Rule rule = new ScheduleSpec.Rule();
        rule.setScope(ScheduleSpec.RuleScope.DAY_OF_WEEK);
        rule.setMode(ScheduleSpec.RuleMode.STRICT);
        rule.setValues(List.of(dayOfWeek));
        spec.setRules(List.of(rule));
        return spec;
    }

    private ScheduleSpec flexibleWeekly(int minCheckins) {
        return baseWeekly(minCheckins);
    }

    private ScheduleSpec dailySchedule() {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setVersion(2);
        spec.setScheduleType(ScheduleSpec.ScheduleType.DAILY);
        spec.setTimezone("Asia/Kolkata");
        return spec;
    }

    private ScheduleSpec monthlyFixedDates(Object... daysOfMonth) {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setVersion(2);
        spec.setScheduleType(ScheduleSpec.ScheduleType.MONTHLY);
        spec.setTimezone("Asia/Kolkata");
        ScheduleSpec.Rule rule = new ScheduleSpec.Rule();
        rule.setScope(ScheduleSpec.RuleScope.DAY_OF_MONTH);
        rule.setMode(ScheduleSpec.RuleMode.STRICT);
        rule.setValues(Arrays.asList(daysOfMonth));
        spec.setRules(List.of(rule));
        return spec;
    }

    private ScheduleSpec quarterlyMonthOfQuarter(int monthOfQuarter) {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setVersion(2);
        spec.setScheduleType(ScheduleSpec.ScheduleType.QUARTERLY);
        spec.setTimezone("Asia/Kolkata");
        ScheduleSpec.Rule rule = new ScheduleSpec.Rule();
        rule.setScope(ScheduleSpec.RuleScope.MONTH_OF_QUARTER);
        rule.setMode(ScheduleSpec.RuleMode.FLEXIBLE);
        rule.setValues(List.of(monthOfQuarter));
        spec.setRules(List.of(rule));
        return spec;
    }

    private ScheduleSpec yearlyDeepRule() {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setVersion(2);
        spec.setScheduleType(ScheduleSpec.ScheduleType.YEARLY);
        spec.setTimezone("Asia/Kolkata");
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
                        values(1),
                        List.of(rule(
                            ScheduleSpec.RuleScope.DAY_OF_WEEK,
                            ScheduleSpec.RuleMode.STRICT,
                            "MONDAY"
                        ))
                    ))
                ))
            )
        ));
        return spec;
    }

    private ScheduleSpec baseWeekly(int minCheckins) {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setVersion(2);
        spec.setScheduleType(ScheduleSpec.ScheduleType.WEEKLY);
        spec.setTimezone("Asia/Kolkata");
        ScheduleSpec.Requirements requirements = new ScheduleSpec.Requirements();
        requirements.setMinCheckins(minCheckins);
        spec.setRequirements(requirements);
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

    private GoalPeriodRepository stubGoalPeriodRepository(List<GoalPeriod> periods) {
        return (GoalPeriodRepository) Proxy.newProxyInstance(
            GoalPeriodRepository.class.getClassLoader(),
            new Class<?>[] {GoalPeriodRepository.class},
            (proxy, method, args) -> {
                String methodName = method.getName();
                if ("findByParentGoalUuidIn".equals(methodName)) {
                    @SuppressWarnings("unchecked")
                    List<String> uuids = (List<String>) args[0];
                    return periods.stream()
                        .filter(period -> uuids.contains(period.getParentGoalUuid()))
                        .toList();
                }
                if ("findByParentGoalUuid".equals(methodName)) {
                    String uuid = (String) args[0];
                    return periods.stream()
                        .filter(period -> uuid.equals(period.getParentGoalUuid()))
                        .toList();
                }
                if ("findActivePeriodForGoal".equals(methodName)) {
                    String uuid = (String) args[0];
                    LocalDate date = (LocalDate) args[1];
                    return periods.stream()
                        .filter(period -> uuid.equals(period.getParentGoalUuid()))
                        .filter(period -> !date.isBefore(period.getPeriodStart()) && !date.isAfter(period.getPeriodEnd()))
                        .findFirst();
                }
                if ("equals".equals(methodName)) {
                    return proxy == args[0];
                }
                if ("hashCode".equals(methodName)) {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(methodName)) {
                    return "StubGoalPeriodRepository";
                }
                if (List.class.equals(method.getReturnType())) {
                    return List.of();
                }
                if (Optional.class.equals(method.getReturnType())) {
                    return Optional.empty();
                }
                if (boolean.class.equals(method.getReturnType())) {
                    return false;
                }
                if (long.class.equals(method.getReturnType())) {
                    return 0L;
                }
                if (int.class.equals(method.getReturnType())) {
                    return 0;
                }
                return null;
            }
        );
    }

    private static class StubGoalService implements GoalService {
        private final List<GoalResponse> goals;

        private StubGoalService(List<GoalResponse> goals) {
            this.goals = goals;
        }

        @Override
        public List<GoalResponse> getAllGoalsByUser(String userId) {
            return goals;
        }

        @Override public GoalResponse createGoal(GoalRequest request, String userId) { throw new UnsupportedOperationException(); }
        @Override public GoalResponse getGoalById(Long id, String userId) { throw new UnsupportedOperationException(); }
        @Override public GoalResponse getGoalByUuid(String uuid, String userId) { throw new UnsupportedOperationException(); }
        @Override public GoalResponse updateGoal(Long id, GoalRequest request, String userId) { throw new UnsupportedOperationException(); }
        @Override public void deleteGoal(Long id, String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> getGoalTree(String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> getChildGoals(String parentGoalId, String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> updateProgressBulk(Map<Long, Double> progressUpdates, String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> updateStatusBulk(Map<Long, Goal.Status> statusUpdates, String userId) { throw new UnsupportedOperationException(); }
        @Override public GoalStatsResponse getGoalStatistics(String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> getOverdueGoals(String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> getDueSoonGoals(String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> getMilestones(String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> searchGoals(String query, String userId) { throw new UnsupportedOperationException(); }
        @Override public GoalResponse updateProgress(Long id, Double currentValue, String userId) { throw new UnsupportedOperationException(); }
        @Override public void recalculateAllProgress(String userId) { throw new UnsupportedOperationException(); }
        @Override public List<GoalResponse> getHealthSummary(String userId) { throw new UnsupportedOperationException(); }
        @Override public GoalResponse recalculateGoalHealth(Long id, String userId) { throw new UnsupportedOperationException(); }
    }

    private static class StubActivityService implements ActivityServiceInterface {
        private final List<ActivityResponse> activities;

        private StubActivityService(List<ActivityResponse> activities) {
            this.activities = activities;
        }

        @Override public Activity create(java.util.HashMap<String, String> activityInfo) { throw new UnsupportedOperationException(); }
        @Override public Activity read(Long id) { throw new UnsupportedOperationException(); }
        @Override public List<ActivityResponse> readAll() { return activities; }
        @Override public List<ActivityResponse> readAll(String userId) { return activities; }
        @Override public void update(java.util.HashMap<String, String> activityInfo) { throw new UnsupportedOperationException(); }
        @Override public void delete(Long id) { throw new UnsupportedOperationException(); }
    }
}
