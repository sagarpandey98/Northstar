package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.ActivityRepository;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Interface.GoalHealthService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodExpectationService;
import com.sagarpandey.activity_tracker.dtos.health.GoalDayHealthDetail;
import com.sagarpandey.activity_tracker.dtos.health.GoalDayExpectation;
import com.sagarpandey.activity_tracker.dtos.health.GoalPeriodHealthBreakdown;
import com.sagarpandey.activity_tracker.dtos.health.GoalPeriodExpectation;
import com.sagarpandey.activity_tracker.dtos.health.MomentumBreakdown;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.Activity;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalHealthServiceV2 implements GoalHealthService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalPeriodRepository goalPeriodRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private GoalPeriodExpectationService goalPeriodExpectationService;

    @Override
    public Double calculateOverallHealthScore(Goal goal) {
        return isParentGoal(goal)
            ? weightedChildAverage(fetchActiveChildren(goal), Goal::getHealthScore)
            : averagePeriodMetric(goal, PeriodSnapshot::healthScore);
    }

    @Override
    public Double calculateConsistencyScore(Goal goal) {
        return isParentGoal(goal)
            ? weightedChildAverage(fetchActiveChildren(goal), Goal::getConsistencyScore)
            : averagePeriodMetric(goal, PeriodSnapshot::consistencyScore);
    }

    @Override
    public Double calculateMomentumScore(Goal goal) {
        return isParentGoal(goal)
            ? weightedChildAverage(fetchActiveChildren(goal), Goal::getMomentumScore)
            : averagePeriodMetric(goal, PeriodSnapshot::momentumScore);
    }

    @Override
    public Double calculateProgressScore(Goal goal) {
        return isParentGoal(goal)
            ? weightedChildAverage(fetchActiveChildren(goal), Goal::getProgressScore)
            : averagePeriodMetric(goal, PeriodSnapshot::progressScore);
    }

    @Override
    public void updateGoalHealth(Goal goal) {
        if (goal == null) {
            return;
        }

        if (isParentGoal(goal)) {
            recalculateSubtree(goal);
        } else {
            recalculateLeafGoal(goal);
        }

        propagateHealthToAncestors(goal);
    }

    @Override
    public Double calculatePeriodConsistencyScore(GoalPeriod period) {
        Goal parentGoal = resolveParentGoal(period);
        PeriodSnapshot snapshot = computeSnapshotForPeriod(parentGoal, period);
        return snapshot != null ? snapshot.consistencyScore() : null;
    }

    @Override
    public Double calculatePeriodProgressScore(GoalPeriod period) {
        Goal parentGoal = resolveParentGoal(period);
        PeriodSnapshot snapshot = computeSnapshotForPeriod(parentGoal, period);
        return snapshot != null ? snapshot.progressScore() : null;
    }

    @Override
    public Double calculatePeriodMomentumScore(GoalPeriod period) {
        Goal parentGoal = resolveParentGoal(period);
        PeriodSnapshot snapshot = computeSnapshotForPeriod(parentGoal, period);
        return snapshot != null ? snapshot.momentumScore() : null;
    }

    @Override
    public Double calculatePeriodOverallHealthScore(GoalPeriod period) {
        Goal parentGoal = resolveParentGoal(period);
        PeriodSnapshot snapshot = computeSnapshotForPeriod(parentGoal, period);
        return snapshot != null ? snapshot.healthScore() : null;
    }

    @Override
    public void updateGoalPeriodHealth(GoalPeriod period) {
        Goal parentGoal = resolveParentGoal(period);
        PeriodSnapshot snapshot = computeSnapshotForPeriod(parentGoal, period);
        if (snapshot == null) {
            return;
        }
        applySnapshotToPeriod(period, snapshot);
        goalPeriodRepository.save(period);
    }

    @Override
    public GoalPeriodHealthBreakdown getPeriodHealthBreakdown(GoalPeriod period, LocalDate evaluationDate) {
        Goal parentGoal = resolveParentGoal(period);
        PeriodSnapshot snapshot = computeSnapshotForPeriod(parentGoal, period, evaluationDate);
        return buildPeriodHealthBreakdown(parentGoal, snapshot);
    }

    @Override
    public Map<String, Object> getHealthBreakdown(Goal goal) {
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("goalUuid", goal.getUuid());
        breakdown.put("overallHealthScore", calculateOverallHealthScore(goal));
        breakdown.put("consistencyScore", calculateConsistencyScore(goal));
        breakdown.put("momentumScore", calculateMomentumScore(goal));
        breakdown.put("progressScore", calculateProgressScore(goal));
        breakdown.put("currentStreak", goal.getCurrentStreak());
        breakdown.put("healthStatus", deriveHealthStatus(calculateOverallHealthScore(goal)));
        return breakdown;
    }

    @Override
    public Map<String, Double> calculateHealthScores(List<Goal> goals) {
        return goals.stream().collect(Collectors.toMap(Goal::getUuid, this::calculateOverallHealthScore));
    }

    @Override
    public Map<String, Object> getHealthStatistics(String userId) {
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGoals", allGoals.size());

        OptionalDouble avgOverall = allGoals.stream()
            .map(this::calculateOverallHealthScore)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average();
        stats.put("averageOverallHealthScore", avgOverall.isPresent() ? round(avgOverall.getAsDouble()) : null);
        return stats;
    }

    @Override
    public boolean needsHealthUpdate(Goal goal) {
        if (goal == null || goal.getLastUpdatedAt() == null) {
            return true;
        }
        return goal.getLastUpdatedAt().isBefore(LocalDateTime.now().minusHours(1));
    }

    private void recalculateSubtree(Goal goal) {
        List<Goal> children = fetchActiveChildren(goal);
        if (children.isEmpty()) {
            recalculateLeafGoal(goal);
            return;
        }

        for (Goal child : children) {
            recalculateSubtree(child);
        }
        rollupParentGoalHealth(goal, children);
        goalRepository.save(goal);
    }

    private void recalculateLeafGoal(Goal goal) {
        List<GoalPeriod> periods = fetchSortedPeriods(goal.getUuid());
        if (periods.isEmpty()) {
            goal.setConsistencyScore(null);
            goal.setMomentumScore(null);
            goal.setProgressScore(null);
            goal.setHealthScore(null);
            goal.setHealthStatus(HealthStatus.UNTRACKED);
            goal.setCurrentStreak(0);
            goalRepository.save(goal);
            return;
        }

        List<PeriodSnapshot> snapshots = computePeriodSnapshots(goal, periods);
        for (PeriodSnapshot snapshot : snapshots) {
            applySnapshotToPeriod(snapshot.period(), snapshot);
            goalPeriodRepository.save(snapshot.period());
        }

        goal.setConsistencyScore(averageSnapshots(snapshots, PeriodSnapshot::consistencyScore));
        goal.setMomentumScore(averageSnapshots(snapshots, PeriodSnapshot::momentumScore));
        goal.setProgressScore(averageSnapshots(snapshots, PeriodSnapshot::progressScore));
        goal.setHealthScore(averageSnapshots(snapshots, PeriodSnapshot::healthScore));
        goal.setHealthStatus(deriveHealthStatus(goal.getHealthScore()));
        updateStreak(goal, snapshots);
        goalRepository.save(goal);
    }

    private void propagateHealthToAncestors(Goal goal) {
        Goal current = goal;
        while (current != null && current.getParentGoalId() != null && !current.getParentGoalId().isBlank()) {
            Goal parent = goalRepository.findByUuidAndIsDeletedFalse(current.getParentGoalId()).orElse(null);
            if (parent == null) {
                return;
            }
            List<Goal> children = fetchActiveChildren(parent);
            rollupParentGoalHealth(parent, children);
            goalRepository.save(parent);
            current = parent;
        }
    }

    private void rollupParentGoalHealth(Goal goal, List<Goal> children) {
        goal.setConsistencyScore(weightedChildAverage(children, Goal::getConsistencyScore));
        goal.setMomentumScore(weightedChildAverage(children, Goal::getMomentumScore));
        goal.setProgressScore(weightedChildAverage(children, Goal::getProgressScore));
        goal.setHealthScore(weightedChildAverage(children, Goal::getHealthScore));
        goal.setHealthStatus(deriveHealthStatus(goal.getHealthScore()));
    }

    private List<PeriodSnapshot> computePeriodSnapshots(Goal goal, List<GoalPeriod> periods) {
        return computePeriodSnapshots(goal, periods, null);
    }

    private List<PeriodSnapshot> computePeriodSnapshots(Goal goal, List<GoalPeriod> periods, LocalDate evaluationDateOverride) {
        if (goal == null || periods == null || periods.isEmpty()) {
            return List.of();
        }

        ZoneId zoneId = resolveZoneId(goal.getScheduleSpec());
        List<Activity> goalActivities = fetchActivitiesForPeriods(goal, periods, zoneId);
        List<PeriodSnapshot> snapshots = new ArrayList<>();

        for (GoalPeriod period : periods) {
            GoalPeriodExpectation expectation = goalPeriodExpectationService.buildExpectation(
                goal,
                period,
                evaluationDateOverride != null ? evaluationDateOverride : resolveEvaluationDate(period, zoneId)
            );
            Map<LocalDate, Double> actualUnitsByDate = aggregateActualUnitsByDate(goal, period, zoneId, goalActivities);
            double actualUnitsToDate = calculateActualUnitsToDate(actualUnitsByDate, expectation.getEvaluationDate());

            Double consistency = calculateExpectationScore(
                expectation.getDailyExpectations(),
                expectation.getEvaluationDate(),
                actualUnitsByDate,
                GoalDayExpectation::getExpectedMinimumUnits
            );
            Double progress = calculateExpectationScore(
                expectation.getDailyExpectations(),
                expectation.getEvaluationDate(),
                actualUnitsByDate,
                GoalDayExpectation::getExpectedTargetUnits
            );
            MomentumAnalysis momentum = analyzeMomentum(consistency, progress, snapshots);
            Double health = calculateOverallHealthScore(goal, consistency, momentum.score(), progress);
            HealthStatus status = derivePeriodHealthStatus(expectation, consistency, progress, health, actualUnitsToDate);

            double currentValue = round(actualUnitsByDate.values().stream().mapToDouble(Double::doubleValue).sum());
            snapshots.add(new PeriodSnapshot(
                period,
                expectation,
                actualUnitsByDate,
                currentValue,
                actualUnitsToDate,
                consistency,
                progress,
                momentum.score(),
                health,
                status,
                momentum
            ));
        }

        return snapshots;
    }

    private PeriodSnapshot computeSnapshotForPeriod(Goal goal, GoalPeriod targetPeriod) {
        return computeSnapshotForPeriod(goal, targetPeriod, null);
    }

    private PeriodSnapshot computeSnapshotForPeriod(Goal goal, GoalPeriod targetPeriod, LocalDate evaluationDateOverride) {
        if (goal == null || targetPeriod == null) {
            return null;
        }
        List<GoalPeriod> periods = fetchSortedPeriods(goal.getUuid());
        if (periods.isEmpty()) {
            return null;
        }
        return computePeriodSnapshots(goal, periods, evaluationDateOverride).stream()
            .filter(snapshot -> snapshot.period().getUuid().equals(targetPeriod.getUuid()))
            .findFirst()
            .orElse(null);
    }

    private List<Activity> fetchActivitiesForPeriods(Goal goal, List<GoalPeriod> periods, ZoneId zoneId) {
        if (goal == null || goal.getId() == null || goal.getUserId() == null || periods == null || periods.isEmpty()) {
            return List.of();
        }

        LocalDate startDate = periods.stream()
            .map(GoalPeriod::getPeriodStart)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
        LocalDate endDate = periods.stream()
            .map(GoalPeriod::getPeriodEnd)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);

        if (startDate == null || endDate == null) {
            return List.of();
        }

        OffsetDateTime periodStartInclusive = startDate.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime periodEndExclusive = endDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        return activityRepository.findGoalActivitiesOverlappingPeriod(
            goal.getId(),
            goal.getUserId(),
            periodStartInclusive,
            periodEndExclusive
        );
    }

    private Map<LocalDate, Double> aggregateActualUnitsByDate(
            Goal goal,
            GoalPeriod period,
            ZoneId zoneId,
            List<Activity> goalActivities) {
        Map<LocalDate, Double> actualUnitsByDate = new LinkedHashMap<>();
        if (goal == null || period == null || goalActivities == null || goalActivities.isEmpty()) {
            return actualUnitsByDate;
        }

        OffsetDateTime periodStartInclusive = period.getPeriodStart().atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime periodEndExclusive = period.getPeriodEnd().plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        for (Activity activity : goalActivities) {
            // Skip / "No activity" records are never counted toward health or progress.
            if (activity == null || activity.isSkip() || activity.getStartTime() == null) {
                continue;
            }

            if (goal.getMetric() == Goal.Metric.DURATION) {
                distributeDurationByDay(activity, zoneId, periodStartInclusive, periodEndExclusive, actualUnitsByDate);
                continue;
            }

            LocalDate localStartDate = activity.getStartTime().atZoneSameInstant(zoneId).toLocalDate();
            if (localStartDate.isBefore(period.getPeriodStart()) || localStartDate.isAfter(period.getPeriodEnd())) {
                continue;
            }
            actualUnitsByDate.merge(localStartDate, 1.0, Double::sum);
        }

        return actualUnitsByDate;
    }

    private void distributeDurationByDay(
            Activity activity,
            ZoneId zoneId,
            OffsetDateTime periodStartInclusive,
            OffsetDateTime periodEndExclusive,
            Map<LocalDate, Double> actualUnitsByDate) {
        OffsetDateTime activityStart = activity.getStartTime();
        OffsetDateTime activityEnd = activity.getEndTime() != null ? activity.getEndTime() : activity.getStartTime();
        if (activityEnd == null || !activityEnd.isAfter(activityStart)) {
            return;
        }

        OffsetDateTime overlapStart = activityStart.isBefore(periodStartInclusive) ? periodStartInclusive : activityStart;
        OffsetDateTime overlapEnd = activityEnd.isAfter(periodEndExclusive) ? periodEndExclusive : activityEnd;
        if (!overlapEnd.isAfter(overlapStart)) {
            return;
        }

        OffsetDateTime cursor = overlapStart;
        while (cursor.isBefore(overlapEnd)) {
            LocalDate localDate = cursor.atZoneSameInstant(zoneId).toLocalDate();
            OffsetDateTime nextBoundary = localDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
            OffsetDateTime segmentEnd = nextBoundary.isBefore(overlapEnd) ? nextBoundary : overlapEnd;
            double minutes = Duration.between(cursor, segmentEnd).toSeconds() / 60.0;
            if (minutes > 0.0) {
                actualUnitsByDate.merge(localDate, minutes, Double::sum);
            }
            cursor = segmentEnd;
        }
    }

    private Double calculateExpectationScore(
            List<GoalDayExpectation> dailyExpectations,
            LocalDate evaluationDate,
            Map<LocalDate, Double> actualUnitsByDate,
            Function<GoalDayExpectation, Double> expectationAccessor) {
        if (dailyExpectations == null || dailyExpectations.isEmpty() || evaluationDate == null) {
            return null;
        }

        double expectedTotal = 0.0;
        double fulfilledTotal = 0.0;
        for (GoalDayExpectation day : dailyExpectations) {
            if (day == null || day.getDate() == null || day.getDate().isAfter(evaluationDate)) {
                continue;
            }
            double expected = sanitize(expectationAccessor.apply(day));
            if (expected <= 0.0) {
                continue;
            }
            expectedTotal += expected;
            double actual = sanitize(actualUnitsByDate.get(day.getDate()));
            fulfilledTotal += Math.min(actual, expected);
        }

        if (expectedTotal <= 0.0) {
            return null;
        }

        return round(Math.min(100.0, (fulfilledTotal / expectedTotal) * 100.0));
    }

    private Double calculateMomentumScore(
            Double consistency,
            Double progress,
            List<PeriodSnapshot> previousSnapshots) {
        return analyzeMomentum(consistency, progress, previousSnapshots).score();
    }

    private MomentumAnalysis analyzeMomentum(
            Double consistency,
            Double progress,
            List<PeriodSnapshot> previousSnapshots) {
        Double currentComposite = averageOf(consistency, progress);
        if (currentComposite == null) {
            return new MomentumAnalysis(null, null, null, 0, "UNTRACKED", "No consistency or progress score is available yet.");
        }
        if (previousSnapshots == null || previousSnapshots.isEmpty()) {
            return new MomentumAnalysis(100.0, currentComposite, null, 0, "FIRST_PERIOD", "First period is treated as full momentum.");
        }

        List<Double> historicalComposites = previousSnapshots.stream()
            .map(snapshot -> averageOf(snapshot.consistencyScore(), snapshot.progressScore()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (historicalComposites.isEmpty()) {
            return new MomentumAnalysis(100.0, currentComposite, null, 0, "NO_BASELINE", "No comparable historical periods are available.");
        }

        int startIndex = Math.max(0, historicalComposites.size() - 2);
        List<Double> baselineWindow = historicalComposites.subList(startIndex, historicalComposites.size());
        double baseline = baselineWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (baseline <= 0.0) {
            Double score = currentComposite > 0.0 ? 100.0 : 0.0;
            String trend = currentComposite > 0.0 ? "RECOVERING" : "FLAT_ZERO";
            return new MomentumAnalysis(score, currentComposite, round(baseline), baselineWindow.size(), trend, "Historical baseline was zero.");
        }
        if (currentComposite >= baseline) {
            return new MomentumAnalysis(100.0, currentComposite, round(baseline), baselineWindow.size(), "IMPROVING", "Current period is at or above the recent baseline.");
        }
        double score = round(Math.min(100.0, (currentComposite / baseline) * 100.0));
        return new MomentumAnalysis(score, currentComposite, round(baseline), baselineWindow.size(), "DECLINING", "Current period is below the recent baseline.");
    }

    private Double calculateOverallHealthScore(
            Goal goal,
            Double consistency,
            Double momentum,
            Double progress) {
        if (goal == null) {
            return null;
        }

        List<WeightedScore> weightedScores = new ArrayList<>();
        if (consistency != null) {
            weightedScores.add(new WeightedScore(consistency, effectiveConsistencyWeight(goal)));
        }
        if (momentum != null) {
            weightedScores.add(new WeightedScore(momentum, effectiveMomentumWeight(goal)));
        }
        if (progress != null) {
            weightedScores.add(new WeightedScore(progress, effectiveProgressWeight(goal)));
        }
        if (weightedScores.isEmpty()) {
            return null;
        }

        double weightedSum = weightedScores.stream().mapToDouble(score -> score.score() * score.weight()).sum();
        int totalWeight = weightedScores.stream().mapToInt(WeightedScore::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }
        return round(Math.min(100.0, weightedSum / totalWeight));
    }

    private HealthStatus derivePeriodHealthStatus(
            GoalPeriodExpectation expectation,
            Double consistency,
            Double progress,
            Double health,
            double actualUnitsToDate) {
        if (expectation == null) {
            return HealthStatus.UNTRACKED;
        }
        boolean noElapsedExpectation = sanitize(expectation.getExpectedMinimumUnitsToDate()) <= 0.0
            && sanitize(expectation.getExpectedTargetUnitsToDate()) <= 0.0;
        if (noElapsedExpectation && actualUnitsToDate <= 0.0) {
            return HealthStatus.UNTRACKED;
        }
        if (consistency == null && progress == null && health == null) {
            return HealthStatus.UNTRACKED;
        }
        return deriveHealthStatus(health);
    }

    private HealthStatus deriveHealthStatus(Double healthScore) {
        if (healthScore == null) {
            return HealthStatus.UNTRACKED;
        }
        if (healthScore >= 80.0) {
            return HealthStatus.THRIVING;
        }
        if (healthScore >= 60.0) {
            return HealthStatus.ON_TRACK;
        }
        if (healthScore >= 40.0) {
            return HealthStatus.AT_RISK;
        }
        return HealthStatus.CRITICAL;
    }

    private void applySnapshotToPeriod(GoalPeriod period, PeriodSnapshot snapshot) {
        period.setCurrentValue(snapshot.currentValue());
        period.setConsistencyScore(snapshot.consistencyScore());
        period.setMomentumScore(snapshot.momentumScore());
        period.setProgressScore(snapshot.progressScore());
        period.setProgressPercentage(snapshot.progressScore());
        period.setHealthScore(snapshot.healthScore());
        period.setHealthStatus(snapshot.healthStatus());
    }

    private GoalPeriodHealthBreakdown buildPeriodHealthBreakdown(Goal goal, PeriodSnapshot snapshot) {
        GoalPeriodHealthBreakdown breakdown = new GoalPeriodHealthBreakdown();
        if (goal == null || snapshot == null) {
            return breakdown;
        }

        GoalPeriod period = snapshot.period();
        GoalPeriodExpectation expectation = snapshot.expectation();

        breakdown.setGoalId(goal.getId());
        breakdown.setGoalUuid(goal.getUuid());
        breakdown.setPeriodUuid(period.getUuid());
        breakdown.setPeriodStart(period.getPeriodStart());
        breakdown.setPeriodEnd(period.getPeriodEnd());
        breakdown.setEvaluationDate(expectation.getEvaluationDate());
        breakdown.setMetric(goal.getMetric());
        breakdown.setUnitLabel(unitLabel(goal));

        breakdown.setConsistencyScore(snapshot.consistencyScore());
        breakdown.setMomentumScore(snapshot.momentumScore());
        breakdown.setProgressScore(snapshot.progressScore());
        breakdown.setHealthScore(snapshot.healthScore());
        breakdown.setHealthStatus(snapshot.healthStatus());

        breakdown.setConsistencyWeight(effectiveConsistencyWeight(goal));
        breakdown.setMomentumWeight(effectiveMomentumWeight(goal));
        breakdown.setProgressWeight(effectiveProgressWeight(goal));

        breakdown.setActionableDayCount(expectation.getActionableDayCount());
        breakdown.setActionableDayCountToDate(expectation.getActionableDayCountToDate());
        breakdown.setTotalExpectedMinimumUnits(expectation.getTotalExpectedMinimumUnits());
        breakdown.setTotalExpectedTargetUnits(expectation.getTotalExpectedTargetUnits());
        breakdown.setExpectedMinimumUnitsToDate(expectation.getExpectedMinimumUnitsToDate());
        breakdown.setExpectedTargetUnitsToDate(expectation.getExpectedTargetUnitsToDate());
        breakdown.setActualUnits(snapshot.currentValue());
        breakdown.setActualUnitsToDate(snapshot.actualUnitsToDate());
        breakdown.setMomentumBreakdown(toMomentumBreakdown(snapshot.momentumAnalysis()));
        breakdown.setDailyDetails(buildDailyDetails(snapshot));
        return breakdown;
    }

    private List<GoalDayHealthDetail> buildDailyDetails(PeriodSnapshot snapshot) {
        if (snapshot == null || snapshot.expectation() == null || snapshot.expectation().getDailyExpectations() == null) {
            return List.of();
        }

        LocalDate evaluationDate = snapshot.expectation().getEvaluationDate();
        return snapshot.expectation().getDailyExpectations().stream()
            .map(day -> buildDailyDetail(day, evaluationDate, snapshot.actualUnitsByDate()))
            .collect(Collectors.toList());
    }

    private GoalDayHealthDetail buildDailyDetail(
            GoalDayExpectation day,
            LocalDate evaluationDate,
            Map<LocalDate, Double> actualUnitsByDate) {
        GoalDayHealthDetail detail = new GoalDayHealthDetail();
        detail.setDate(day.getDate());
        detail.setActionable(day.isActionable());
        detail.setCountedInScore(evaluationDate != null && day.getDate() != null && !day.getDate().isAfter(evaluationDate));

        double expectedMinimum = sanitize(day.getExpectedMinimumUnits());
        double expectedTarget = sanitize(day.getExpectedTargetUnits());
        double actual = sanitize(actualUnitsByDate != null ? actualUnitsByDate.get(day.getDate()) : null);
        double consistencyFulfilled = Math.min(actual, expectedMinimum);
        double progressFulfilled = Math.min(actual, expectedTarget);

        detail.setExpectedMinimumUnits(round(expectedMinimum));
        detail.setExpectedTargetUnits(round(expectedTarget));
        detail.setActualUnits(round(actual));
        detail.setConsistencyFulfilledUnits(round(consistencyFulfilled));
        detail.setProgressFulfilledUnits(round(progressFulfilled));
        detail.setConsistencyScore(expectedMinimum > 0.0 ? round((consistencyFulfilled / expectedMinimum) * 100.0) : null);
        detail.setProgressScore(expectedTarget > 0.0 ? round((progressFulfilled / expectedTarget) * 100.0) : null);
        return detail;
    }

    private MomentumBreakdown toMomentumBreakdown(MomentumAnalysis analysis) {
        if (analysis == null) {
            return null;
        }

        MomentumBreakdown breakdown = new MomentumBreakdown();
        breakdown.setCurrentCompositeScore(analysis.currentCompositeScore());
        breakdown.setBaselineCompositeScore(analysis.baselineCompositeScore());
        breakdown.setPeriodsCompared(analysis.periodsCompared());
        breakdown.setTrend(analysis.trend());
        breakdown.setExplanation(analysis.explanation());
        if (analysis.currentCompositeScore() != null && analysis.baselineCompositeScore() != null) {
            breakdown.setDeltaFromBaseline(round(analysis.currentCompositeScore() - analysis.baselineCompositeScore()));
        }
        return breakdown;
    }

    private String unitLabel(Goal goal) {
        if (goal == null || goal.getMetric() == null) {
            return "units";
        }
        return goal.getMetric() == Goal.Metric.DURATION ? "minutes" : "activities";
    }

    private void updateStreak(Goal goal, List<PeriodSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            goal.setCurrentStreak(0);
            return;
        }

        int streak = 0;
        int missesAllowed = goal.getMissesAllowedPerPeriod() != null ? goal.getMissesAllowedPerPeriod() : 0;
        int missesUsed = 0;
        int longestStreak = goal.getLongestStreak() != null ? goal.getLongestStreak() : 0;

        for (PeriodSnapshot snapshot : snapshots) {
            boolean hit = snapshot.consistencyScore() != null && snapshot.consistencyScore() >= 100.0;
            if (hit) {
                streak++;
                longestStreak = Math.max(longestStreak, streak);
            } else if (snapshot.healthStatus() != HealthStatus.UNTRACKED) {
                missesUsed++;
                if (missesUsed > missesAllowed) {
                    streak = 0;
                    missesUsed = 0;
                }
            }
        }

        goal.setCurrentStreak(streak);
        goal.setLongestStreak(longestStreak);
    }

    private Double averagePeriodMetric(Goal goal, Function<PeriodSnapshot, Double> extractor) {
        if (goal == null) {
            return null;
        }
        List<GoalPeriod> periods = fetchSortedPeriods(goal.getUuid());
        return averageSnapshots(computePeriodSnapshots(goal, periods), extractor);
    }

    private Double averageSnapshots(List<PeriodSnapshot> snapshots, Function<PeriodSnapshot, Double> extractor) {
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }
        OptionalDouble average = snapshots.stream()
            .map(extractor)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average();
        return average.isPresent() ? round(average.getAsDouble()) : null;
    }

    private Double weightedChildAverage(List<Goal> children, Function<Goal, Double> extractor) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        for (Goal child : children) {
            Double value = extractor.apply(child);
            if (value == null) {
                continue;
            }
            double weight = getPriorityWeight(child.getPriority());
            weightedSum += value * weight;
            totalWeight += weight;
        }
        if (totalWeight <= 0.0) {
            return null;
        }
        return round(Math.min(100.0, weightedSum / totalWeight));
    }

    private double getPriorityWeight(Goal.Priority priority) {
        if (priority == null) {
            return 1.0;
        }
        return switch (priority) {
            case CRITICAL -> 4.0;
            case HIGH -> 3.0;
            case MEDIUM -> 2.0;
            case LOW -> 1.0;
        };
    }

    private boolean isParentGoal(Goal goal) {
        return !fetchActiveChildren(goal).isEmpty();
    }

    private List<Goal> fetchActiveChildren(Goal goal) {
        if (goal == null || goal.getUuid() == null || goal.getUserId() == null) {
            return List.of();
        }
        return goalRepository.findByParentGoalIdAndUserIdAndIsDeletedFalse(goal.getUuid(), goal.getUserId());
    }

    private List<GoalPeriod> fetchSortedPeriods(String goalUuid) {
        if (goalUuid == null) {
            return List.of();
        }
        return goalPeriodRepository.findByParentGoalUuid(goalUuid).stream()
            .sorted(Comparator.comparing(GoalPeriod::getPeriodStart))
            .collect(Collectors.toList());
    }

    private Goal resolveParentGoal(GoalPeriod period) {
        if (period == null) {
            return null;
        }
        if (period.getGoal() != null) {
            return period.getGoal();
        }
        return goalRepository.findByUuidAndIsDeletedFalse(period.getParentGoalUuid()).orElse(null);
    }

    private LocalDate resolveEvaluationDate(GoalPeriod period, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        if (period == null || period.getPeriodStart() == null || period.getPeriodEnd() == null) {
            return today;
        }
        if (today.isBefore(period.getPeriodStart())) {
            return today;
        }
        return today.isAfter(period.getPeriodEnd()) ? period.getPeriodEnd() : today;
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

    private double calculateActualUnitsToDate(Map<LocalDate, Double> actualUnitsByDate, LocalDate evaluationDate) {
        if (actualUnitsByDate == null || actualUnitsByDate.isEmpty() || evaluationDate == null) {
            return 0.0;
        }
        return round(actualUnitsByDate.entrySet().stream()
            .filter(entry -> entry.getKey() != null && !entry.getKey().isAfter(evaluationDate))
            .mapToDouble(entry -> sanitize(entry.getValue()))
            .sum());
    }

    private Double averageOf(Double left, Double right) {
        List<Double> values = new ArrayList<>();
        if (left != null) {
            values.add(left);
        }
        if (right != null) {
            values.add(right);
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private int effectiveConsistencyWeight(Goal goal) {
        return goal.getConsistencyWeight() != null ? goal.getConsistencyWeight() : goal.getEffectiveConsistencyWeight();
    }

    private int effectiveMomentumWeight(Goal goal) {
        return goal.getMomentumWeight() != null ? goal.getMomentumWeight() : goal.getEffectiveMomentumWeight();
    }

    private int effectiveProgressWeight(Goal goal) {
        return goal.getProgressWeight() != null ? goal.getProgressWeight() : goal.getEffectiveProgressWeight();
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

    private record WeightedScore(Double score, int weight) {}

    private record MomentumAnalysis(
        Double score,
        Double currentCompositeScore,
        Double baselineCompositeScore,
        int periodsCompared,
        String trend,
        String explanation
    ) {}

    private record PeriodSnapshot(
        GoalPeriod period,
        GoalPeriodExpectation expectation,
        Map<LocalDate, Double> actualUnitsByDate,
        double currentValue,
        double actualUnitsToDate,
        Double consistencyScore,
        Double progressScore,
        Double momentumScore,
        Double healthScore,
        HealthStatus healthStatus,
        MomentumAnalysis momentumAnalysis
    ) {}
}
