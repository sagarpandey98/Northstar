package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Service.Interface.ActivityServiceInterface;
import com.sagarpandey.activity_tracker.Service.Interface.GoalService;
import com.sagarpandey.activity_tracker.Service.Interface.SmartTodoService;
import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.SmartTodoListResponse;
import com.sagarpandey.activity_tracker.dtos.SmartTodoResponse;
import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import com.sagarpandey.activity_tracker.utils.ScheduleSpecEvaluator;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Smart Todo V1.1
 *
 * This service keeps the implementation deterministic and explainable:
 * - respects the new schedule_spec V2 rules
 * - uses period-aware pacing instead of one opaque score
 * - exposes reasons/buckets so frontend can group and explain items
 */
@Service
public class SmartTodoServiceV1 implements SmartTodoService {

    private static final Logger log = LoggerFactory.getLogger(SmartTodoServiceV1.class);
    private static final double EPSILON = 0.0001;

    private final GoalService goalService;
    private final ActivityServiceInterface activityService;
    private final GoalPeriodRepository goalPeriodRepository;

    @Autowired
    public SmartTodoServiceV1(
            GoalService goalService,
            ActivityServiceInterface activityService,
            GoalPeriodRepository goalPeriodRepository) {
        this.goalService = goalService;
        this.activityService = activityService;
        this.goalPeriodRepository = goalPeriodRepository;
    }

    @Override
    public SmartTodoListResponse getTodaySmartTodos(String userId) {
        return buildSmartTodoList(userId, null, "TODAY");
    }

    @Override
    public SmartTodoListResponse refreshTodayTodos(String userId) {
        return getTodaySmartTodos(userId);
    }

    @Override
    public SmartTodoListResponse getSmartTodosForDate(String userId, LocalDate date) {
        return buildSmartTodoList(userId, date, "DATE");
    }

    private SmartTodoListResponse buildSmartTodoList(String userId, LocalDate requestedDate, String listType) {
        try {
            List<GoalResponse> allGoals = safeGoalList(goalService.getAllGoalsByUser(userId));
            ZoneId userZone = resolveUserZone(allGoals);
            LocalDate todayInUserZone = LocalDate.now(userZone);
            LocalDate targetDate = requestedDate != null ? requestedDate : todayInUserZone;

            List<ActivityResponse> allActivities = safeActivityList(activityService.readAll(userId));
            Map<Long, List<ActivityResponse>> activitiesByGoalId = allActivities.stream()
                .filter(activity -> activity.getGoalId() != null)
                .collect(Collectors.groupingBy(ActivityResponse::getGoalId));

            List<String> goalUuids = allGoals.stream()
                .map(GoalResponse::getUuid)
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .distinct()
                .toList();

            Map<String, List<GoalPeriod>> periodsByGoalUuid = new HashMap<>();
            if (!goalUuids.isEmpty()) {
                periodsByGoalUuid = goalPeriodRepository.findByParentGoalUuidIn(goalUuids).stream()
                    .collect(Collectors.groupingBy(GoalPeriod::getParentGoalUuid));
            }

            List<SmartTodoResponse> items = new ArrayList<>();
            for (GoalResponse goal : allGoals) {
                if (!isTrackableGoal(goal)) {
                    continue;
                }
                List<GoalPeriod> goalPeriods = periodsByGoalUuid.getOrDefault(goal.getUuid(), Collections.emptyList());
                GoalPeriod activePeriod = resolvePeriodForDate(goal, goalPeriods, targetDate);
                ScheduleSpec effectiveSpec = effectiveScheduleSpec(goal, activePeriod);

                SmartTodoResponse todo = buildTodoItem(
                    goal,
                    goalPeriods,
                    activitiesByGoalId.getOrDefault(goal.getId(), Collections.emptyList()),
                    targetDate,
                    todayInUserZone,
                    userZone
                );

                if (todo != null && shouldIncludeTodo(todo, hasExplicitSchedule(effectiveSpec))) {
                    items.add(todo);
                }
            }

            List<SmartTodoResponse> sortedItems = sortSmartTodos(items);
            applyDisplayRanksAndFocus(sortedItems);

            SmartTodoListResponse response = new SmartTodoListResponse();
            response.setDate(targetDate);
            response.setTimezone(userZone.getId());
            response.setListType(listType);
            response.setItems(sortedItems);
            response.setSummary(buildSummary(sortedItems));
            response.setGeneratedAt(LocalDateTime.now(userZone));
            return response;
        } catch (Exception e) {
            log.error("Failed to build smart todo list for userId={} date={}", userId, requestedDate, e);
            throw new IllegalStateException("Unable to generate smart todo list", e);
        }
    }

    private SmartTodoResponse buildTodoItem(
            GoalResponse goal,
            List<GoalPeriod> goalPeriods,
            List<ActivityResponse> goalActivities,
            LocalDate targetDate,
            LocalDate actualToday,
            ZoneId userZone) {
        boolean futureDateView = targetDate.isAfter(actualToday);
        GoalPeriod activePeriod = resolvePeriodForDate(goal, goalPeriods, targetDate);
        if (activePeriod == null) {
            return null;
        }
        ScheduleSpec spec = effectiveScheduleSpec(goal, activePeriod);

        boolean explicitSchedule = hasExplicitSchedule(spec);
        boolean scheduledForToday = isScheduledForDate(spec, targetDate);
        boolean mustHappenToday = isHardScheduledForDate(spec, scheduledForToday);
        boolean periodStartsToday = activePeriod != null && targetDate.equals(activePeriod.getPeriodStart());
        LocalDate referenceDate = futureDateView ? actualToday : targetDate;

        String progressUnit = resolveProgressUnit(goal.getMetric());
        int todayProgress = toWholeNumber(sumProgressOnDate(goal, goalActivities, targetDate, userZone));
        int periodCurrentProgress = activePeriod != null
            ? toWholeNumber(sumProgressBetween(goal, goalActivities, activePeriod.getPeriodStart(), referenceDate, userZone))
            : todayProgress;
        int periodDurationProgressMinutes = activePeriod != null
            ? toWholeNumber(sumDurationBetween(goalActivities, activePeriod.getPeriodStart(), referenceDate, userZone))
            : toWholeNumber(sumDurationBetween(goalActivities, targetDate, referenceDate, userZone));
        int progressBeforeTargetDate = activePeriod != null
            ? toWholeNumber(sumProgressBetween(
                goal,
                goalActivities,
                activePeriod.getPeriodStart(),
                minDate(targetDate.minusDays(1), actualToday),
                userZone))
            : 0;

        int totalActionableDays = activePeriod != null
            ? ScheduleSpecEvaluator.countActionableDays(activePeriod.getPeriodStart(), activePeriod.getPeriodEnd(), spec)
            : 0;
        int elapsedActionableDays = activePeriod != null
            ? ScheduleSpecEvaluator.countActionableDays(activePeriod.getPeriodStart(), referenceDate, spec)
            : 0;
        int elapsedActionableDaysForTarget = activePeriod != null
            ? ScheduleSpecEvaluator.countActionableDays(activePeriod.getPeriodStart(), targetDate, spec)
            : 0;
        int remainingActionableDays = activePeriod != null
            ? ScheduleSpecEvaluator.countActionableDays(targetDate, activePeriod.getPeriodEnd(), spec)
            : 0;

        int periodTargetProgress = toWholeNumber(resolvePeriodTargetValue(goal, activePeriod, totalActionableDays, spec));
        int expectedByReferenceDate = (activePeriod != null && totalActionableDays > 0 && periodTargetProgress > 0)
            ? (int) Math.floor(periodTargetProgress * (elapsedActionableDays / (double) totalActionableDays))
            : 0;
        int expectedByTargetDate = (activePeriod != null && totalActionableDays > 0 && periodTargetProgress > 0)
            ? (int) Math.floor(periodTargetProgress * (elapsedActionableDaysForTarget / (double) totalActionableDays))
            : 0;

        boolean behindSchedule = !futureDateView
            && activePeriod != null
            && periodTargetProgress > 0
            && periodCurrentProgress + EPSILON < expectedByReferenceDate;

        int baseTodayTarget = calculateBaseTodayTarget(
            goal,
            activePeriod,
            periodTargetProgress,
            periodDurationProgressMinutes,
            remainingActionableDays
        );
        int catchUpTarget = futureDateView ? 0 : Math.max(0, expectedByTargetDate - progressBeforeTargetDate);
        int todayTarget = Math.max(baseTodayTarget, catchUpTarget);
        if (todayTarget <= 0) {
            todayTarget = goal.getMetric() == Goal.Metric.DURATION
                ? Math.max(15, calculateDefaultSuggestedTime(goal))
                : 1;
        }

        boolean completedToday = todayProgress >= todayTarget;
        int remainingTodayTarget = Math.max(0, todayTarget - todayProgress);
        int remainingPeriodTarget = Math.max(0, periodTargetProgress - periodCurrentProgress);
        double paceRatio = expectedByReferenceDate > 0
            ? periodCurrentProgress / (double) expectedByReferenceDate
            : 1.0;

        boolean streakAtRisk = !futureDateView && isStreakAtRisk(spec, goal, targetDate, goalActivities, userZone);
        Integer baselineDailyTimeCommitment = calculateBaselineDailyTimeCommitment(goal, activePeriod, totalActionableDays);
        int suggestedTimeMinutes = calculateSuggestedTime(
            goal,
            activePeriod,
            periodDurationProgressMinutes,
            remainingActionableDays,
            todayTarget
        );

        List<String> reasonCodes = new ArrayList<>();
        List<String> reasonMessages = new ArrayList<>();

        if (mustHappenToday) {
            addReason(
                reasonCodes,
                reasonMessages,
                "SCHEDULED_TODAY",
                "Scheduled for this date via " + describeSchedulePath(spec, targetDate)
            );
        }
        if (streakAtRisk) {
            addReason(
                reasonCodes,
                reasonMessages,
                "STREAK_AT_RISK",
                "Skipping this breaks the current streak."
            );
        }
        if (periodStartsToday) {
            addReason(
                reasonCodes,
                reasonMessages,
                "PERIOD_STARTS_TODAY",
                "This goal period starts on this date."
            );
        }
        if (behindSchedule) {
            addReason(
                reasonCodes,
                reasonMessages,
                "BEHIND_PACE",
                buildBehindPaceMessage(periodCurrentProgress, expectedByReferenceDate, progressUnit)
            );
        }
        if (todayProgress > 0 && !completedToday) {
            addReason(
                reasonCodes,
                reasonMessages,
                "PROGRESS_STARTED",
                "Progress already started for this date."
            );
        }
        if (completedToday) {
            addReason(
                reasonCodes,
                reasonMessages,
                "TODAY_TARGET_MET",
                "The recommended target for this date is already met."
            );
        }

        int daysUntilTargetDate = calculateDaysUntilTargetDate(goal, targetDate);
        if (daysUntilTargetDate != Integer.MAX_VALUE && daysUntilTargetDate <= 7) {
            addReason(
                reasonCodes,
                reasonMessages,
                "DEADLINE_NEAR",
                buildDeadlineMessage(daysUntilTargetDate)
            );
        }

        if (reasonCodes.isEmpty()) {
            addReason(
                reasonCodes,
                reasonMessages,
                explicitSchedule ? "ON_TRACK" : "FLEXIBLE_TODAY",
                explicitSchedule
                    ? "On pace for this schedule right now."
                    : "Flexible today; a small session keeps momentum healthy."
            );
        }

        String todoStatus = determineTodoStatus(mustHappenToday, streakAtRisk, behindSchedule, completedToday);
        double urgencyScore = calculateUrgencyScore(goal, todoStatus, streakAtRisk, behindSchedule, daysUntilTargetDate, goal.getHealthScore(), todayProgress);
        List<SmartTodoResponse.ScoreComponent> scoreBreakdown = buildScoreBreakdown(
            goal,
            todoStatus,
            streakAtRisk,
            behindSchedule,
            daysUntilTargetDate,
            goal.getHealthScore(),
            todayProgress > 0
        );

        SmartTodoResponse todo = new SmartTodoResponse();
        todo.setGoalId(goal.getId());
        todo.setTitle(goal.getTitle());
        todo.setDescription(goal.getDescription());
        todo.setPriority(goal.getPriority());
        todo.setGoalType(goal.getGoalType());
        todo.setPriorityDisplay(buildPriorityDisplay(goal.getPriority()));
        todo.setScheduledForToday(scheduledForToday);
        todo.setScheduleType(spec != null && spec.getScheduleType() != null ? spec.getScheduleType().name() : "UNSCHEDULED");
        todo.setScheduleDetails(describeSchedulePath(spec, targetDate));
        todo.setScheduleLabel(buildScheduleLabel(spec, targetDate));
        todo.setCurrentProgress(todayProgress);
        todo.setTargetProgress(todayTarget);
        todo.setProgressPercentage(calculatePercentage(todayProgress, todayTarget));
        todo.setCompletedToday(completedToday);
        todo.setProgressUnit(progressUnit);
        todo.setProgressDisplay(formatProgressDisplay(todayProgress, todayTarget, progressUnit));
        todo.setCurrentStreak(goal.getCurrentStreak());
        todo.setStreakAtRisk(streakAtRisk);
        todo.setBehindSchedule(behindSchedule);
        todo.setUrgencyReason(reasonMessages.get(0));
        todo.setMinimumSessionPeriod(resolvePeriodTimeCommitment(goal, activePeriod));
        todo.setMinimumSessionDaily(baselineDailyTimeCommitment);
        todo.setSuggestedTimeMinutes(suggestedTimeMinutes);
        todo.setLastCompletedDate(getLastActivityDate(goalActivities, userZone));
        todo.setRequiresQuickLog(!completedToday);
        todo.setQuickLogContext(goal.getTitle());
        todo.setTodoStatus(todoStatus);
        todo.setPrimaryReasonCode(reasonCodes.get(0));
        todo.setReasonCodes(reasonCodes);
        todo.setReasonMessages(reasonMessages);
        todo.setScoreBreakdown(scoreBreakdown);
        todo.setRecommendedAction(buildRecommendedAction(todoStatus, remainingTodayTarget, progressUnit, suggestedTimeMinutes));
        todo.setRemainingTodayTarget(remainingTodayTarget);
        todo.setRemainingPeriodTarget(remainingPeriodTarget);
        todo.setExpectedProgressByToday(expectedByTargetDate);
        todo.setPaceRatio(roundToTwoDecimals(paceRatio));
        todo.setPeriodCurrentProgress(periodCurrentProgress);
        todo.setPeriodTargetProgress(periodTargetProgress);
        todo.setPeriodProgressPercentage(calculatePercentage(periodCurrentProgress, periodTargetProgress));
        todo.setTotalActionableDays(totalActionableDays > 0 ? totalActionableDays : null);
        todo.setElapsedActionableDays(elapsedActionableDaysForTarget > 0 ? elapsedActionableDaysForTarget : null);
        todo.setRemainingActionableDays(remainingActionableDays > 0 ? remainingActionableDays : null);
        todo.setPeriodStartDate(activePeriod != null ? activePeriod.getPeriodStart().toString() : null);
        todo.setPeriodEndDate(activePeriod != null ? activePeriod.getPeriodEnd().toString() : null);
        todo.setPeriodStartsToday(periodStartsToday);
        todo.setDaysUntilTargetDate(daysUntilTargetDate == Integer.MAX_VALUE ? null : daysUntilTargetDate);
        todo.setHealthScoreSnapshot(goal.getHealthScore());
        todo.setUrgencyScore(roundToTwoDecimals(urgencyScore));
        todo.setSmartPriorityGroup(buildPriorityGroup(todoStatus));
        todo.setCreatedAt(goal.getCreatedAt());
        todo.setLastUpdatedAt(goal.getLastUpdatedAt());
        return todo;
    }

    private boolean isTrackableGoal(GoalResponse goal) {
        if (goal == null) {
            return false;
        }
        if (Boolean.TRUE.equals(goal.getIsMilestone())) {
            return false;
        }
        if (Boolean.FALSE.equals(goal.getIsTracked())) {
            return false;
        }
        if (goal.getStatus() == Goal.Status.COMPLETED) {
            return false;
        }
        return goal.getIsLeaf() == null || goal.getIsLeaf();
    }

    private boolean shouldIncludeTodo(SmartTodoResponse todo, boolean explicitSchedule) {
        if (todo == null) {
            return false;
        }
        if (todo.getCurrentProgress() != null && todo.getCurrentProgress() > 0) {
            return true;
        }
        if (todo.isScheduledForToday()) {
            return true;
        }
        if (todo.isPeriodStartsToday()) {
            return true;
        }
        if (explicitSchedule) {
            return false;
        }
        return todo.isStreakAtRisk() || todo.isBehindSchedule();
    }

    private List<SmartTodoResponse> sortSmartTodos(List<SmartTodoResponse> todos) {
        return todos.stream()
            .sorted(
                Comparator.comparingInt((SmartTodoResponse todo) -> todoStatusRank(todo.getTodoStatus()))
                    .thenComparing(
                        (SmartTodoResponse todo) -> safeDouble(todo.getUrgencyScore()),
                        Comparator.reverseOrder()
                    )
                    .thenComparing(
                        (SmartTodoResponse todo) -> priorityWeight(todo.getPriority()),
                        Comparator.reverseOrder()
                    )
                    .thenComparing(
                        SmartTodoResponse::getTitle,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    )
            )
            .collect(Collectors.toList());
    }

    private void applyDisplayRanksAndFocus(List<SmartTodoResponse> items) {
        int focusAssigned = 0;
        for (int i = 0; i < items.size(); i++) {
            SmartTodoResponse todo = items.get(i);
            todo.setDisplayRank(i + 1);
            boolean focus = !todo.isCompletedToday() && focusAssigned < 3;
            todo.setRecommendedFocus(focus);
            if (focus) {
                focusAssigned++;
            }
        }
    }

    private SmartTodoListResponse.Summary buildSummary(List<SmartTodoResponse> items) {
        SmartTodoListResponse.Summary summary = new SmartTodoListResponse.Summary();
        summary.setTotalItems(items.size());
        summary.setMustDoTodayCount((int) items.stream().filter(todo -> "MUST_DO_TODAY".equals(todo.getTodoStatus())).count());
        summary.setCatchUpTodayCount((int) items.stream().filter(todo -> "CATCH_UP_TODAY".equals(todo.getTodoStatus())).count());
        summary.setGoodToDoTodayCount((int) items.stream().filter(todo -> "GOOD_TO_DO_TODAY".equals(todo.getTodoStatus())).count());
        summary.setCompletedTodayCount((int) items.stream().filter(todo -> "COMPLETED_TODAY".equals(todo.getTodoStatus())).count());
        summary.setRecommendedFocusGoalIds(
            items.stream()
                .filter(SmartTodoResponse::isRecommendedFocus)
                .map(SmartTodoResponse::getGoalId)
                .filter(Objects::nonNull)
                .toList()
        );
        summary.setRecommendedFocusTitles(
            items.stream()
                .filter(SmartTodoResponse::isRecommendedFocus)
                .map(SmartTodoResponse::getTitle)
                .filter(Objects::nonNull)
                .toList()
        );
        return summary;
    }

    private int calculateBaseTodayTarget(
            GoalResponse goal,
            GoalPeriod activePeriod,
            int periodTargetProgress,
            int periodDurationProgressMinutes,
            int remainingActionableDays) {
        if (goal.getMetric() == Goal.Metric.DURATION) {
            Integer dailyCommitment = calculateTodayTimeCommitment(
                goal,
                activePeriod,
                periodDurationProgressMinutes,
                remainingActionableDays
            );
            if (dailyCommitment != null && dailyCommitment > 0) {
                return dailyCommitment;
            }
        }

        if (activePeriod != null && periodTargetProgress > 0 && remainingActionableDays > 0) {
            int fallback = (int) Math.ceil(periodTargetProgress / (double) remainingActionableDays);
            return Math.max(1, fallback);
        }

        if (goal.getMetric() == Goal.Metric.DURATION) {
            return Math.max(15, calculateDefaultSuggestedTime(goal));
        }
        return 1;
    }

    private boolean isStreakAtRisk(
            ScheduleSpec spec,
            GoalResponse goal,
            LocalDate targetDate,
            List<ActivityResponse> goalActivities,
            ZoneId userZone) {
        if (goal.getCurrentStreak() == null || goal.getCurrentStreak() <= 0) {
            return false;
        }
        LocalDate yesterday = targetDate.minusDays(1);
        if (!isScheduledForDate(spec, yesterday)) {
            return false;
        }
        return countCheckinsOnDate(goalActivities, yesterday, userZone) == 0;
    }

    private double calculateUrgencyScore(
            GoalResponse goal,
            String todoStatus,
            boolean streakAtRisk,
            boolean behindSchedule,
            int daysUntilTargetDate,
            Double healthScore,
            int todayProgress) {
        double score = switch (safePriority(goal.getPriority())) {
            case CRITICAL -> 30.0;
            case HIGH -> 22.0;
            case MEDIUM -> 14.0;
            case LOW -> 8.0;
        };

        score += switch (todoStatus) {
            case "MUST_DO_TODAY" -> 30.0;
            case "CATCH_UP_TODAY" -> 20.0;
            case "GOOD_TO_DO_TODAY" -> 10.0;
            case "COMPLETED_TODAY" -> -20.0;
            default -> 0.0;
        };

        if (streakAtRisk) {
            score += 18.0;
        }
        if (behindSchedule) {
            score += 12.0;
        }
        if (daysUntilTargetDate != Integer.MAX_VALUE) {
            if (daysUntilTargetDate < 0) {
                score += 16.0;
            } else if (daysUntilTargetDate <= 1) {
                score += 12.0;
            } else if (daysUntilTargetDate <= 3) {
                score += 8.0;
            } else if (daysUntilTargetDate <= 7) {
                score += 4.0;
            }
        }
        if (healthScore != null) {
            if (healthScore < 40) {
                score += 8.0;
            } else if (healthScore < 60) {
                score += 4.0;
            }
        }
        if (todayProgress > 0) {
            score += 2.0;
        }
        return Math.max(0.0, Math.min(100.0, score));
    }

    private List<SmartTodoResponse.ScoreComponent> buildScoreBreakdown(
            GoalResponse goal,
            String todoStatus,
            boolean streakAtRisk,
            boolean behindSchedule,
            int daysUntilTargetDate,
            Double healthScore,
            boolean progressStarted) {
        List<SmartTodoResponse.ScoreComponent> components = new ArrayList<>();
        components.add(new SmartTodoResponse.ScoreComponent(
            "PRIORITY",
            "Goal priority",
            switch (safePriority(goal.getPriority())) {
                case CRITICAL -> 30.0;
                case HIGH -> 22.0;
                case MEDIUM -> 14.0;
                case LOW -> 8.0;
            }
        ));

        if ("MUST_DO_TODAY".equals(todoStatus)) {
            components.add(new SmartTodoResponse.ScoreComponent("STATUS", "Must-do bucket", 30.0));
        } else if ("CATCH_UP_TODAY".equals(todoStatus)) {
            components.add(new SmartTodoResponse.ScoreComponent("STATUS", "Catch-up bucket", 20.0));
        } else if ("GOOD_TO_DO_TODAY".equals(todoStatus)) {
            components.add(new SmartTodoResponse.ScoreComponent("STATUS", "Good-to-do bucket", 10.0));
        } else if ("COMPLETED_TODAY".equals(todoStatus)) {
            components.add(new SmartTodoResponse.ScoreComponent("STATUS", "Completed bucket", -20.0));
        }

        if (streakAtRisk) {
            components.add(new SmartTodoResponse.ScoreComponent("STREAK_AT_RISK", "Streak pressure", 18.0));
        }
        if (behindSchedule) {
            components.add(new SmartTodoResponse.ScoreComponent("BEHIND_PACE", "Behind pace", 12.0));
        }
        if (daysUntilTargetDate != Integer.MAX_VALUE) {
            if (daysUntilTargetDate < 0) {
                components.add(new SmartTodoResponse.ScoreComponent("OVERDUE_DEADLINE", "Past target date", 16.0));
            } else if (daysUntilTargetDate <= 1) {
                components.add(new SmartTodoResponse.ScoreComponent("DEADLINE_PRESSURE", "Deadline within 1 day", 12.0));
            } else if (daysUntilTargetDate <= 3) {
                components.add(new SmartTodoResponse.ScoreComponent("DEADLINE_PRESSURE", "Deadline within 3 days", 8.0));
            } else if (daysUntilTargetDate <= 7) {
                components.add(new SmartTodoResponse.ScoreComponent("DEADLINE_PRESSURE", "Deadline within 7 days", 4.0));
            }
        }
        if (healthScore != null) {
            if (healthScore < 40) {
                components.add(new SmartTodoResponse.ScoreComponent("LOW_HEALTH", "Low goal health", 8.0));
            } else if (healthScore < 60) {
                components.add(new SmartTodoResponse.ScoreComponent("LOW_HEALTH", "Soft health warning", 4.0));
            }
        }
        if (progressStarted) {
            components.add(new SmartTodoResponse.ScoreComponent("PROGRESS_STARTED", "Already in motion", 2.0));
        }
        return components;
    }

    private int calculateSuggestedTime(
            GoalResponse goal,
            GoalPeriod activePeriod,
            int periodDurationProgressMinutes,
            int remainingActionableDays,
            int todayTarget) {
        Integer commitmentBasedTime = calculateTodayTimeCommitment(
            goal,
            activePeriod,
            periodDurationProgressMinutes,
            remainingActionableDays
        );
        if (commitmentBasedTime != null && commitmentBasedTime > 0) {
            return goal.getMetric() == Goal.Metric.DURATION && todayTarget > 0
                ? Math.max(todayTarget, commitmentBasedTime)
                : commitmentBasedTime;
        }

        if (goal.getMetric() == Goal.Metric.DURATION && todayTarget > 0) {
            return todayTarget;
        }

        if (activePeriod != null) {
            if (activePeriod.getMinimumSessionDaily() != null && activePeriod.getMinimumSessionDaily() > 0) {
                return (int) Math.ceil(activePeriod.getMinimumSessionDaily());
            }

            Integer periodMinimum = activePeriod.getMinimumSessionPeriod() != null
                ? activePeriod.getMinimumSessionPeriod()
                : goal.getMinimumSessionPeriod();
            if (periodMinimum != null && periodMinimum > 0) {
                if (remainingActionableDays > 0) {
                    return Math.max(1, (int) Math.ceil(periodMinimum / (double) remainingActionableDays));
                }
                return periodMinimum;
            }
        }

        Integer dailyFloor = resolveDailyTimeFloor(goal, activePeriod);
        if (dailyFloor != null && dailyFloor > 0) {
            return dailyFloor;
        }

        return calculateDefaultSuggestedTime(goal);
    }

    private Integer calculateBaselineDailyTimeCommitment(
            GoalResponse goal,
            GoalPeriod activePeriod,
            int totalActionableDays) {
        Integer periodCommitment = resolvePeriodTimeCommitment(goal, activePeriod);
        if (periodCommitment != null && periodCommitment > 0 && totalActionableDays > 0) {
            return Math.max(1, (int) Math.ceil(periodCommitment / (double) totalActionableDays));
        }
        return resolveDailyTimeFloor(goal, activePeriod);
    }

    private Integer calculateTodayTimeCommitment(
            GoalResponse goal,
            GoalPeriod activePeriod,
            int periodDurationProgressMinutes,
            int remainingActionableDays) {
        Integer dailyFloor = resolveDailyTimeFloor(goal, activePeriod);
        Integer periodCommitment = resolvePeriodTimeCommitment(goal, activePeriod);
        if (periodCommitment != null && periodCommitment > 0) {
            int remainingCommitment = Math.max(0, periodCommitment - Math.max(0, periodDurationProgressMinutes));
            int actionableDays = Math.max(1, remainingActionableDays);
            int commitmentPerDay = remainingCommitment > 0
                ? Math.max(1, (int) Math.ceil(remainingCommitment / (double) actionableDays))
                : 0;
            if (dailyFloor != null && dailyFloor > 0) {
                return commitmentPerDay > 0 ? Math.max(commitmentPerDay, dailyFloor) : dailyFloor;
            }
            return commitmentPerDay > 0 ? commitmentPerDay : null;
        }
        return dailyFloor;
    }

    private Integer resolvePeriodTimeCommitment(GoalResponse goal, GoalPeriod activePeriod) {
        return firstPositiveInteger(
            goal != null ? goal.getMinimumTimeCommittedPeriod() : null,
            activePeriod != null ? activePeriod.getMinimumSessionPeriod() : null,
            goal != null ? goal.getMinimumSessionPeriod() : null
        );
    }

    private Integer resolveDailyTimeFloor(GoalResponse goal, GoalPeriod activePeriod) {
        return firstPositiveInteger(
            activePeriod != null && activePeriod.getMinimumSessionDaily() != null
                ? Integer.valueOf((int) Math.ceil(activePeriod.getMinimumSessionDaily()))
                : null,
            goal != null ? goal.getMinimumTimeCommittedDaily() : null
        );
    }

    private Integer firstPositiveInteger(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private int calculateDefaultSuggestedTime(GoalResponse goal) {
        int suggestedTime = 30;
        if (goal.getGoalType() == GoalType.PROJECT) {
            suggestedTime = Math.max(suggestedTime, 45);
        } else if (goal.getGoalType() == GoalType.SKILL) {
            suggestedTime = Math.max(suggestedTime, 60);
        } else if (goal.getGoalType() == GoalType.FITNESS) {
            suggestedTime = Math.max(suggestedTime, 40);
        }
        return suggestedTime;
    }

    private double resolvePeriodTargetValue(
            GoalResponse goal,
            GoalPeriod activePeriod,
            int totalActionableDays,
            ScheduleSpec spec) {
        if (goal.getMetric() == Goal.Metric.DURATION) {
            return firstPositive(
                activePeriod != null ? toDouble(activePeriod.getMinimumSessionPeriod()) : null,
                toDouble(goal.getMinimumSessionPeriod()),
                toDouble(goal.getMinimumTimeCommittedPeriod()),
                goal.getTargetValue()
            );
        }

        if (goal.getMetric() == Goal.Metric.CUSTOM) {
            return firstPositive(goal.getTargetValue(), activePeriod != null ? activePeriod.getCurrentValue() : null, 1.0);
        }

        Integer minCheckins = getMinCheckinsRequired(spec);
        if (minCheckins != null && minCheckins > 0) {
            return minCheckins;
        }
        if (totalActionableDays > 0) {
            return totalActionableDays;
        }
        return 1.0;
    }

    private Integer getMinCheckinsRequired(ScheduleSpec spec) {
        if (spec == null || spec.getRequirements() == null) {
            return null;
        }
        return spec.getRequirements().getMinCheckins();
    }

    private boolean hasExplicitSchedule(ScheduleSpec spec) {
        return spec != null && spec.getRules() != null && !spec.getRules().isEmpty();
    }

    private boolean isHardScheduledForDate(ScheduleSpec spec, boolean scheduledForDate) {
        if (spec == null || !scheduledForDate) {
            return false;
        }
        if (spec.getScheduleType() == ScheduleSpec.ScheduleType.DAILY) {
            return true;
        }
        return hasExplicitSchedule(spec);
    }

    private boolean isScheduledForDate(ScheduleSpec spec, LocalDate date) {
        return spec == null || ScheduleSpecEvaluator.isActionable(spec, date);
    }

    private ScheduleSpec effectiveScheduleSpec(GoalResponse goal, GoalPeriod activePeriod) {
        if (activePeriod != null && activePeriod.getScheduleSpec() != null) {
            return activePeriod.getScheduleSpec();
        }
        return goal != null ? goal.getScheduleSpec() : null;
    }

    private Optional<GoalPeriod> findActivePeriod(List<GoalPeriod> periods, LocalDate date) {
        if (periods == null || periods.isEmpty() || date == null) {
            return Optional.empty();
        }
        return periods.stream()
            .filter(period -> period.getPeriodStart() != null && period.getPeriodEnd() != null)
            .filter(period -> !date.isBefore(period.getPeriodStart()) && !date.isAfter(period.getPeriodEnd()))
            .findFirst();
    }
    
    private GoalPeriod resolvePeriodForDate(GoalResponse goal, List<GoalPeriod> periods, LocalDate targetDate) {
        if (goal == null || targetDate == null) {
            return null;
        }
        if (goal.getStartDate() != null && targetDate.isBefore(goal.getStartDate().toLocalDate())) {
            return null;
        }
        Optional<GoalPeriod> active = findActivePeriod(periods, targetDate);
        if (active.isPresent()) {
            return active.get();
        }
        return buildSyntheticPeriod(goal, targetDate);
    }
    
    private GoalPeriod buildSyntheticPeriod(GoalResponse goal, LocalDate targetDate) {
        LocalDate periodStart;
        LocalDate periodEnd;
        ScheduleSpec spec = goal.getScheduleSpec();
        ScheduleSpec.ScheduleType scheduleType = spec != null ? spec.getScheduleType() : null;
        
        if (scheduleType == null) {
            periodStart = targetDate.withDayOfMonth(1);
            periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        } else {
            switch (scheduleType) {
                case DAILY -> {
                    periodStart = targetDate;
                    periodEnd = targetDate;
                }
                case WEEKLY -> {
                    DayOfWeek weekStartsOn = resolveWeekStartsOn(spec);
                    periodStart = targetDate.with(TemporalAdjusters.previousOrSame(weekStartsOn));
                    periodEnd = periodStart.plusDays(6);
                }
                case QUARTERLY -> {
                    int quarter = (targetDate.getMonthValue() - 1) / 3;
                    int startMonth = quarter * 3 + 1;
                    periodStart = targetDate.withMonth(startMonth).withDayOfMonth(1);
                    LocalDate endMonth = periodStart.plusMonths(2);
                    periodEnd = endMonth.withDayOfMonth(endMonth.lengthOfMonth());
                }
                case YEARLY -> {
                    periodStart = targetDate.withDayOfYear(1);
                    periodEnd = targetDate.withDayOfYear(targetDate.lengthOfYear());
                }
                case MONTHLY -> {
                    periodStart = targetDate.withDayOfMonth(1);
                    periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
                }
                default -> {
                    periodStart = targetDate.withDayOfMonth(1);
                    periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
                }
            }
        }
        
        if (goal.getStartDate() != null) {
            LocalDate goalStart = goal.getStartDate().toLocalDate();
            if (periodStart.isBefore(goalStart)) {
                periodStart = goalStart;
            }
            if (periodEnd.isBefore(periodStart)) {
                return null;
            }
        }
        
        GoalPeriod synthetic = new GoalPeriod();
        synthetic.setParentGoalUuid(goal.getUuid());
        synthetic.setPeriodStart(periodStart);
        synthetic.setPeriodEnd(periodEnd);
        synthetic.setMetric(goal.getMetric());
        synthetic.setTargetOperator(goal.getTargetOperator());
        synthetic.setMinimumSessionPeriod(goal.getMinimumSessionPeriod());
        synthetic.setMaximumSessionPeriod(goal.getMaximumSessionPeriod());
        synthetic.setAllowDoubleLogging(goal.getAllowDoubleLogging());
        synthetic.setScheduleSpec(goal.getScheduleSpec());
        synthetic.setCurrentValue(0.0);
        return synthetic;
    }
    
    private DayOfWeek resolveWeekStartsOn(ScheduleSpec spec) {
        if (spec == null || spec.getWeekStartsOn() == null || spec.getWeekStartsOn().isBlank()) {
            return DayOfWeek.MONDAY;
        }
        try {
            return DayOfWeek.valueOf(spec.getWeekStartsOn().toUpperCase());
        } catch (Exception e) {
            return DayOfWeek.MONDAY;
        }
    }

    private ZoneId resolveUserZone(List<GoalResponse> goals) {
        for (GoalResponse goal : goals) {
            if (goal.getScheduleSpec() == null || goal.getScheduleSpec().getTimezone() == null) {
                continue;
            }
            try {
                return ZoneId.of(goal.getScheduleSpec().getTimezone());
            } catch (Exception ignored) {
                // Keep scanning until we find a valid zone.
            }
        }
        return ZoneId.systemDefault();
    }

    private String resolveProgressUnit(Goal.Metric metric) {
        if (metric == Goal.Metric.DURATION) {
            return "minutes";
        }
        if (metric == Goal.Metric.CUSTOM) {
            return "units";
        }
        return "check-ins";
    }

    private double sumProgressOnDate(
            GoalResponse goal,
            List<ActivityResponse> goalActivities,
            LocalDate date,
            ZoneId userZone) {
        return goalActivities.stream()
            .filter(activity -> isActivityOnDate(activity, date, userZone))
            .mapToDouble(activity -> measureActivityProgress(goal.getMetric(), activity))
            .sum();
    }

    private double sumProgressBetween(
            GoalResponse goal,
            List<ActivityResponse> goalActivities,
            LocalDate start,
            LocalDate end,
            ZoneId userZone) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0.0;
        }
        return goalActivities.stream()
            .map(activity -> Map.entry(activity, resolveActivityDate(activity, userZone)))
            .filter(entry -> entry.getValue() != null)
            .filter(entry -> !entry.getValue().isBefore(start) && !entry.getValue().isAfter(end))
            .mapToDouble(entry -> measureActivityProgress(goal.getMetric(), entry.getKey()))
            .sum();
    }
    
    private double sumDurationBetween(
            List<ActivityResponse> goalActivities,
            LocalDate start,
            LocalDate end,
            ZoneId userZone) {
        if (goalActivities == null || start == null || end == null || end.isBefore(start)) {
            return 0.0;
        }
        return goalActivities.stream()
            .map(activity -> Map.entry(activity, resolveActivityDate(activity, userZone)))
            .filter(entry -> entry.getValue() != null)
            .filter(entry -> !entry.getValue().isBefore(start) && !entry.getValue().isAfter(end))
            .mapToDouble(entry -> parseDurationMinutes(entry.getKey()))
            .sum();
    }

    private long countCheckinsOnDate(List<ActivityResponse> goalActivities, LocalDate date, ZoneId userZone) {
        return goalActivities.stream()
            .filter(activity -> isActivityOnDate(activity, date, userZone))
            .count();
    }

    private long countCheckinsBetween(
            List<ActivityResponse> goalActivities,
            LocalDate start,
            LocalDate end,
            ZoneId userZone) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        return goalActivities.stream()
            .map(activity -> resolveActivityDate(activity, userZone))
            .filter(Objects::nonNull)
            .filter(date -> !date.isBefore(start) && !date.isAfter(end))
            .count();
    }

    private boolean isActivityOnDate(ActivityResponse activity, LocalDate targetDate, ZoneId userZone) {
        LocalDate activityDate = resolveActivityDate(activity, userZone);
        return targetDate != null && targetDate.equals(activityDate);
    }

    private LocalDate resolveActivityDate(ActivityResponse activity, ZoneId userZone) {
        if (activity == null) {
            return null;
        }
        if (activity.getStartTime() != null) {
            return activity.getStartTime().atZoneSameInstant(userZone).toLocalDate();
        }
        if (activity.getCreated_at() != null && activity.getCreated_at().length() >= 10) {
            try {
                return LocalDate.parse(activity.getCreated_at().substring(0, 10));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }

    private double measureActivityProgress(Goal.Metric metric, ActivityResponse activity) {
        if (metric == Goal.Metric.DURATION) {
            return parseDurationMinutes(activity);
        }
        return 1.0;
    }

    private double parseDurationMinutes(ActivityResponse activity) {
        if (activity == null) {
            return 0.0;
        }
        if (activity.getStartTime() != null && activity.getEndTime() != null) {
            long minutes = Duration.between(activity.getStartTime(), activity.getEndTime()).toMinutes();
            if (minutes > 0) {
                return minutes;
            }
        }
        String duration = activity.getDuration();
        if (duration == null || duration.isBlank()) {
            return 0.0;
        }
        try {
            return Duration.parse(duration).toMinutes();
        } catch (Exception ignored) {
            // Fall through to custom string parsing.
        }
        String normalized = duration.toLowerCase(Locale.ROOT).trim();
        if (normalized.matches("\\d+")) {
            return Double.parseDouble(normalized);
        }

        long hours = extractLeadingNumber(normalized, "hour");
        long minutes = extractLeadingNumber(normalized, "minute");
        if (hours > 0 || minutes > 0) {
            return hours * 60.0 + minutes;
        }
        return 0.0;
    }

    private long extractLeadingNumber(String text, String unitStem) {
        int unitIndex = text.indexOf(unitStem);
        if (unitIndex <= 0) {
            return 0;
        }
        String numberPortion = text.substring(0, unitIndex).replaceAll("[^0-9]", " ").trim();
        if (numberPortion.isBlank()) {
            return 0;
        }
        String[] parts = numberPortion.split("\\s+");
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String getLastActivityDate(List<ActivityResponse> goalActivities, ZoneId userZone) {
        return goalActivities.stream()
            .map(activity -> resolveActivityDate(activity, userZone))
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .map(LocalDate::toString)
            .orElse(null);
    }

    private String determineTodoStatus(
            boolean mustHappenToday,
            boolean streakAtRisk,
            boolean behindSchedule,
            boolean completedToday) {
        if (completedToday) {
            return "COMPLETED_TODAY";
        }
        if (streakAtRisk || mustHappenToday) {
            return "MUST_DO_TODAY";
        }
        if (behindSchedule) {
            return "CATCH_UP_TODAY";
        }
        return "GOOD_TO_DO_TODAY";
    }

    private String buildPriorityGroup(String todoStatus) {
        return switch (todoStatus) {
            case "MUST_DO_TODAY" -> "MUST DO";
            case "CATCH_UP_TODAY" -> "CATCH UP";
            case "GOOD_TO_DO_TODAY" -> "GOOD TO DO";
            case "COMPLETED_TODAY" -> "DONE";
            default -> "SMART TODO";
        };
    }

    private int todoStatusRank(String todoStatus) {
        return switch (todoStatus) {
            case "MUST_DO_TODAY" -> 0;
            case "CATCH_UP_TODAY" -> 1;
            case "GOOD_TO_DO_TODAY" -> 2;
            case "COMPLETED_TODAY" -> 3;
            default -> 4;
        };
    }

    private int priorityWeight(Goal.Priority priority) {
        return switch (safePriority(priority)) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private void addReason(List<String> codes, List<String> messages, String code, String message) {
        if (!codes.contains(code)) {
            codes.add(code);
            messages.add(message);
        }
    }

    private String buildRecommendedAction(String todoStatus, int remainingTodayTarget, String progressUnit, int suggestedTimeMinutes) {
        if ("COMPLETED_TODAY".equals(todoStatus)) {
            return "Already covered for this date.";
        }
        if (remainingTodayTarget > 0) {
            return progressUnit.equals("minutes")
                ? "Give this goal " + remainingTodayTarget + " more minutes."
                : "Log " + remainingTodayTarget + " more " + singularize(progressUnit, remainingTodayTarget) + ".";
        }
        return "A focused " + suggestedTimeMinutes + "-minute session keeps this on track.";
    }

    private String buildBehindPaceMessage(int actual, int expected, String progressUnit) {
        return "Behind pace: " + actual + " vs " + expected + " expected " + progressUnit + " by now.";
    }

    private String buildDeadlineMessage(int daysUntilTargetDate) {
        if (daysUntilTargetDate < 0) {
            return "Target date has already passed.";
        }
        if (daysUntilTargetDate == 0) {
            return "Target date is today.";
        }
        if (daysUntilTargetDate == 1) {
            return "Target date is tomorrow.";
        }
        return "Target date is in " + daysUntilTargetDate + " days.";
    }

    private int calculateDaysUntilTargetDate(GoalResponse goal, LocalDate targetDate) {
        if (goal.getTargetDate() == null || targetDate == null) {
            return Integer.MAX_VALUE;
        }
        return (int) ChronoUnit.DAYS.between(targetDate, goal.getTargetDate().toLocalDate());
    }

    private String buildPriorityDisplay(Goal.Priority priority) {
        return switch (safePriority(priority)) {
            case CRITICAL -> "P1";
            case HIGH -> "P2";
            case MEDIUM -> "P3";
            case LOW -> "P4";
        };
    }

    private String buildScheduleLabel(ScheduleSpec spec, LocalDate date) {
        if (spec == null) {
            return "Flexible schedule";
        }
        List<String> pieces = new ArrayList<>();
        if (spec.getScheduleType() != null) {
            pieces.add(spec.getScheduleType().name());
        }
        String path = describeSchedulePath(spec, date);
        if (!path.isBlank()) {
            pieces.add(path);
        }
        Integer minCheckins = getMinCheckinsRequired(spec);
        if (minCheckins != null && minCheckins > 0) {
            pieces.add(minCheckins + " check-ins / period");
        }
        return String.join(" • ", pieces);
    }

    private String describeSchedulePath(ScheduleSpec spec, LocalDate date) {
        if (spec == null || spec.getRules() == null || spec.getRules().isEmpty() || date == null) {
            return "";
        }
        for (ScheduleSpec.Rule rule : spec.getRules()) {
            List<String> path = describeMatchingRulePath(spec, rule, date);
            if (!path.isEmpty()) {
                return String.join(" > ", path);
            }
        }
        return "";
    }

    private List<String> describeMatchingRulePath(ScheduleSpec spec, ScheduleSpec.Rule rule, LocalDate date) {
        if (rule == null || rule.getScope() == null || !matchesRuleForDate(spec, rule, date)) {
            return Collections.emptyList();
        }

        List<String> path = new ArrayList<>();
        path.add(formatRuleLabel(spec, rule, date));
        if (rule.getMode() == ScheduleSpec.RuleMode.FLEXIBLE || rule.getRules() == null || rule.getRules().isEmpty()) {
            return path;
        }

        for (ScheduleSpec.Rule child : rule.getRules()) {
            List<String> childPath = describeMatchingRulePath(spec, child, date);
            if (!childPath.isEmpty()) {
                path.addAll(childPath);
                return path;
            }
        }
        return path;
    }

    private boolean matchesRuleForDate(ScheduleSpec spec, ScheduleSpec.Rule rule, LocalDate date) {
        return switch (rule.getScope()) {
            case QUARTER -> containsInt(rule.getValues(), quarterOfYear(date));
            case MONTH_OF_YEAR -> containsInt(rule.getValues(), date.getMonthValue());
            case MONTH_OF_QUARTER -> containsInt(rule.getValues(), monthOfQuarter(date));
            case WEEK_OF_MONTH -> containsInt(rule.getValues(), weekOfMonth(date, spec));
            case DAY_OF_MONTH -> containsDayOfMonth(rule.getValues(), date);
            case DAY_OF_WEEK -> containsName(rule.getValues(), date.getDayOfWeek().name());
            case TIME_OF_DAY -> rule.getValues() != null && !rule.getValues().isEmpty();
            case TIME_WINDOW -> rule.getWindows() != null && !rule.getWindows().isEmpty();
        };
    }

    private String formatRuleLabel(ScheduleSpec spec, ScheduleSpec.Rule rule, LocalDate date) {
        return switch (rule.getScope()) {
            case QUARTER -> "Q" + quarterOfYear(date);
            case MONTH_OF_YEAR -> date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH);
            case MONTH_OF_QUARTER -> "M" + monthOfQuarter(date);
            case WEEK_OF_MONTH -> "W" + weekOfMonth(date, spec);
            case DAY_OF_MONTH -> "D" + date.getDayOfMonth();
            case DAY_OF_WEEK -> date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH);
            case TIME_OF_DAY -> rule.getValues().stream().map(Object::toString).collect(Collectors.joining(", "));
            case TIME_WINDOW -> rule.getWindows().stream()
                .map(window -> window.getStart() + "-" + window.getEnd())
                .collect(Collectors.joining(", "));
        };
    }

    private int weekOfMonth(LocalDate date, ScheduleSpec spec) {
        if (spec != null && spec.getWeekOfMonthModel() == ScheduleSpec.WeekOfMonthModel.CALENDAR_WEEKS) {
            LocalDate monthStart = date.withDayOfMonth(1);
            int offset = Math.floorMod(
                monthStart.getDayOfWeek().getValue() - resolveWeekStartsOn(spec.getWeekStartsOn()).getValue(),
                7
            );
            return (date.getDayOfMonth() + offset - 1) / 7 + 1;
        }
        return (date.getDayOfMonth() - 1) / 7 + 1;
    }

    private java.time.DayOfWeek resolveWeekStartsOn(String weekStartsOn) {
        if (weekStartsOn == null || weekStartsOn.isBlank()) {
            return java.time.DayOfWeek.MONDAY;
        }
        try {
            return java.time.DayOfWeek.valueOf(weekStartsOn.toUpperCase(Locale.ENGLISH));
        } catch (Exception ignored) {
            return java.time.DayOfWeek.MONDAY;
        }
    }

    private int quarterOfYear(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    private int monthOfQuarter(LocalDate date) {
        return (date.getMonthValue() - 1) % 3 + 1;
    }

    private boolean containsInt(List<Object> values, int expected) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.stream().anyMatch(value -> parseInt(value, Integer.MIN_VALUE) == expected);
    }

    private boolean containsName(List<Object> values, String expected) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.stream().anyMatch(value -> value != null && expected.equalsIgnoreCase(value.toString()));
    }

    private boolean containsDayOfMonth(List<Object> values, LocalDate date) {
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
            if (parseInt(value, Integer.MIN_VALUE) == date.getDayOfMonth()) {
                return true;
            }
        }
        return false;
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalDate minDate(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }

    private Goal.Priority safePriority(Goal.Priority priority) {
        return priority != null ? priority : Goal.Priority.MEDIUM;
    }

    private List<GoalResponse> safeGoalList(List<GoalResponse> goals) {
        return goals != null ? goals : Collections.emptyList();
    }

    private List<ActivityResponse> safeActivityList(List<ActivityResponse> activities) {
        return activities != null ? activities : Collections.emptyList();
    }

    private double calculatePercentage(int current, int target) {
        if (target <= 0) {
            return 0.0;
        }
        return roundToTwoDecimals((current * 100.0) / target);
    }

    private int toWholeNumber(double value) {
        return (int) Math.max(0, Math.round(value));
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private String formatProgressDisplay(int current, int target, String unit) {
        return current + " / " + target + " " + unit;
    }

    private String singularize(String unit, int count) {
        if (count == 1) {
            if ("check-ins".equals(unit)) {
                return "check-in";
            }
            if ("minutes".equals(unit)) {
                return "minute";
            }
            if ("units".equals(unit)) {
                return "unit";
            }
        }
        return unit;
    }

    private Double toDouble(Integer value) {
        return value != null ? value.doubleValue() : null;
    }

    private double firstPositive(Double... values) {
        for (Double value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return 0.0;
    }
}
