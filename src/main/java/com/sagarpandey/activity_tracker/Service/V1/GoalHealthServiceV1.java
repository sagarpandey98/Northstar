package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.GoalPeriodSnapshotRepository;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Repository.GoalWeeklySnapshotRepository;
import com.sagarpandey.activity_tracker.Service.Inteface.GoalHealthService;
import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriodSnapshot;
import com.sagarpandey.activity_tracker.models.GoalWeeklySnapshot;
import com.sagarpandey.activity_tracker.utils.PeriodUtils;
import com.sagarpandey.activity_tracker.utils.WeekUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GoalHealthServiceV1 implements GoalHealthService {

    private static final Logger log = 
        LoggerFactory.getLogger(GoalHealthServiceV1.class);

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalWeeklySnapshotRepository snapshotRepository;

    @Autowired
    private GoalPeriodSnapshotRepository periodSnapshotRepository;

    // =========================================================
    // PUBLIC API
    // =========================================================

    @Override
    public void onActivityLogged(Long goalId, LocalDate activityDate) {
        try {
            updateWeeklySnapshot(goalId, activityDate);
            updatePeriodSnapshot(goalId, activityDate); // ADD THIS
            recalculateHealth(goalId);
        } catch (Exception e) {
            // Non-blocking — activity creation must not fail
            // if health calculation fails
            log.error("Failed to update health for goalId={}: {}",
                goalId, e.getMessage(), e);
        }
    }

    @Override
    public void recalculateHealth(Long goalId) {
        Goal goal = goalRepository.findById(goalId).orElse(null);
        if (goal == null || Boolean.TRUE.equals(goal.getIsDeleted())) {
            log.warn("Goal not found or deleted: {}", goalId);
            return;
        }

        // STEP A: Consistency Score
        Double consistencyScore = calculateConsistencyScore(goal);

        // STEP B: Momentum Score
        Double momentumScore = calculateMomentumScore(goal);

        // STEP C: Progress Score (time-paced)
        Double progressScore = calculateProgressScore(goal);

        // STEP D: Final Health Score
        Double healthScore = calculateHealthScore(
            goal, consistencyScore, momentumScore, progressScore
        );

        // STEP E: Health Status
        HealthStatus healthStatus = deriveHealthStatus(healthScore);

        // STEP F: Streak
        updateStreak(goal);

        // STEP G: Persist
        goal.setConsistencyScore(consistencyScore);
        goal.setMomentumScore(momentumScore);
        goal.setHealthScore(healthScore);
        goal.setHealthStatus(healthStatus);
        goal.setLastUpdatedAt(LocalDateTime.now());

        goalRepository.save(goal);

        log.debug("Health recalculated for goalId={}: " +
            "consistency={}, momentum={}, progress={}, health={}",
            goalId, consistencyScore, momentumScore, 
            progressScore, healthScore);
    }

    @Override
    public void recalculateAllHealthForUser(String userId) {
        List<Goal> activeGoals = goalRepository
            .findByUserIdAndIsDeletedFalse(userId);

        for (Goal goal : activeGoals) {
            try {
                recalculateHealth(goal.getId());
            } catch (Exception e) {
                log.error("Failed to recalculate health for " +
                    "goalId={}: {}", goal.getId(), e.getMessage());
            }
        }
    }

    // =========================================================
    // WEEKLY SNAPSHOT
    // =========================================================

    private void updateWeeklySnapshot(
            Long goalId, LocalDate activityDate) {

        Goal goal = goalRepository.findById(goalId).orElse(null);
        if (goal == null) return;

        LocalDate weekStart = WeekUtils.getMondayOf(activityDate);

        // Check double logging rule
        if (!Boolean.TRUE.equals(goal.getAllowDoubleLogging())) {
            // Count activities already logged today for this goal
            // If allowDoubleLogging is false and an activity was
            // already logged today, skip incrementing
            // We check by seeing if activitiesLogged already
            // accounts for today — simplified check:
            // if today is same week and activities >= days elapsed
            // this is handled at activity creation level in Phase 3
            // Here we just proceed with snapshot update
        }

        Optional<GoalWeeklySnapshot> existing =
            snapshotRepository.findByGoalIdAndWeekStart(
                goalId, weekStart
            );

        GoalWeeklySnapshot snapshot;
        if (existing.isPresent()) {
            snapshot = existing.get();
            snapshot.setActivitiesLogged(
                snapshot.getActivitiesLogged() + 1
            );
        } else {
            snapshot = new GoalWeeklySnapshot();
            snapshot.setGoalId(goalId);
            snapshot.setUserId(goal.getUserId());
            snapshot.setWeekStart(weekStart);
            snapshot.setActivitiesLogged(1);
            snapshot.setTargetFrequencyWeekly(
                goal.getTargetFrequencyWeekly()
            );
        }

        // Recalculate consistency score for this snapshot
        Double weekScore = WeekUtils.calculateConsistencyScore(
            snapshot.getActivitiesLogged(),
            goal.getTargetFrequencyWeekly()
        );
        snapshot.setConsistencyScoreForWeek(weekScore);
        snapshot.setTargetFrequencyWeekly(
            goal.getTargetFrequencyWeekly()
        );

        snapshotRepository.save(snapshot);
    }

    // =========================================================
    // PERIOD SNAPSHOT (PHASE 9)
    // =========================================================

    private void updatePeriodSnapshot(
            Long goalId, LocalDate activityDate) {

        Goal goal = goalRepository.findById(goalId).orElse(null);
        if (goal == null) return;

        // Only run if evaluationPeriod is configured
        // and is NOT WEEKLY (weekly is handled by existing system)
        if (goal.getEvaluationPeriod() == null
                || goal.getEvaluationPeriod()
                    == EvaluationPeriod.WEEKLY) {
            return;
        }

        EvaluationPeriod periodType = goal.getEvaluationPeriod();
        Integer customDays = goal.getCustomPeriodDays();

        // Compute period start for the activity date
        LocalDate periodStart = PeriodUtils.getPeriodStart(
            activityDate,
            periodType,
            customDays,
            goal.getCurrentPeriodStart()
        );

        LocalDate periodEnd = PeriodUtils.getPeriodEnd(
            periodStart, periodType, customDays
        );

        // Find or create snapshot for this period
        Optional<GoalPeriodSnapshot> existing =
            periodSnapshotRepository
                .findByGoalIdAndPeriodTypeAndPeriodStart(
                    goalId, periodType, periodStart
                );

        GoalPeriodSnapshot snapshot;
        if (existing.isPresent()) {
            snapshot = existing.get();
            snapshot.setActivitiesLogged(
                snapshot.getActivitiesLogged() + 1
            );
        } else {
            snapshot = new GoalPeriodSnapshot();
            snapshot.setGoalId(goalId);
            snapshot.setUserId(goal.getUserId());
            snapshot.setPeriodType(periodType);
            snapshot.setPeriodStart(periodStart);
            snapshot.setPeriodEnd(periodEnd);
            snapshot.setActivitiesLogged(1);
            snapshot.setTargetPerPeriod(goal.getTargetPerPeriod());
        }

        // Recalculate consistency score for this snapshot
        Double score = PeriodUtils.calculatePeriodConsistencyScore(
            snapshot.getActivitiesLogged(),
            goal.getTargetPerPeriod()
        );
        snapshot.setConsistencyScore(score);
        snapshot.setTargetPerPeriod(goal.getTargetPerPeriod());
        periodSnapshotRepository.save(snapshot);

        // Update goal's current period tracking fields
        goal.setCurrentPeriodStart(periodStart);
        goal.setCurrentPeriodCount(snapshot.getActivitiesLogged());
        goal.setPeriodConsistencyScore(score);
        goalRepository.save(goal);
    }

    // =========================================================
    // CONSISTENCY SCORE
    // =========================================================

    private Double calculateConsistencyScore(Goal goal) {

        // === PHASE 9: Period-based consistency ===
        // If goal has a non-weekly evaluation period configured,
        // use period consistency score instead of weekly
        if (goal.getEvaluationPeriod() != null
                && goal.getEvaluationPeriod()
                    != EvaluationPeriod.WEEKLY) {

            // Use stored periodConsistencyScore if available
            if (goal.getPeriodConsistencyScore() != null) {
                return goal.getPeriodConsistencyScore();
            }

            // If no score yet, return null (untracked)
            return null;
        }
        // === END PHASE 9 ===

        // === EXISTING WEEKLY LOGIC (unchanged) ===
        if (goal.getTargetFrequencyWeekly() == null
                || goal.getTargetFrequencyWeekly() == 0) {
            return null;
        }

        LocalDate weekStart = WeekUtils.getCurrentWeekMonday();
        Optional<GoalWeeklySnapshot> currentWeekSnapshot =
            snapshotRepository.findByGoalIdAndWeekStart(
                goal.getId(), weekStart
            );

        int activitiesThisWeek = currentWeekSnapshot
            .map(GoalWeeklySnapshot::getActivitiesLogged)
            .orElse(0);

        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        double expectedByNow = goal.getTargetFrequencyWeekly()
            * (dayOfWeek / 7.0);

        if (expectedByNow == 0) expectedByNow = 1;

        double score = (activitiesThisWeek / expectedByNow) * 100.0;
        return Math.min(100.0, score);
        // === END EXISTING WEEKLY LOGIC ===
    }

    // =========================================================
    // MOMENTUM SCORE
    // =========================================================

    private Double calculateMomentumScore(Goal goal) {
        if (goal.getTargetFrequencyWeekly() == null
                || goal.getTargetFrequencyWeekly() == 0) {
            return null; // untracked
        }

        // Fetch last 4 weeks of snapshots
        LocalDate currentMonday = WeekUtils.getCurrentWeekMonday();
        LocalDate fourWeeksAgo = WeekUtils.weeksAgo(3);

        List<GoalWeeklySnapshot> snapshots =
            snapshotRepository.findByGoalIdAndWeekStartBetween(
                goal.getId(), fourWeeksAgo, currentMonday
            );

        // Build week scores array [week-3, week-2, week-1, current]
        // with weights              [0.10,   0.20,   0.30,  0.40]
        double[] weights = {0.10, 0.20, 0.30, 0.40};
        double[] weekScores = new double[4];

        for (int i = 0; i < 4; i++) {
            LocalDate weekDate = WeekUtils.weeksAgo(3 - i);
            weekScores[i] = snapshots.stream()
                .filter(s -> s.getWeekStart().equals(weekDate))
                .findFirst()
                .map(s -> {
                    if (s.getConsistencyScoreForWeek() != null) {
                        return s.getConsistencyScoreForWeek();
                    }
                    return WeekUtils.calculateConsistencyScore(
                        s.getActivitiesLogged(),
                        goal.getTargetFrequencyWeekly()
                    ) != null ? WeekUtils.calculateConsistencyScore(
                        s.getActivitiesLogged(),
                        goal.getTargetFrequencyWeekly()
                    ) : 0.0;
                })
                .orElse(0.0); // no snapshot = 0
        }

        // Weighted rolling average
        double momentumScore = 0.0;
        for (int i = 0; i < 4; i++) {
            momentumScore += weekScores[i] * weights[i];
        }

        // Apply streak multiplier
        momentumScore = applyStreakMultiplier(
            momentumScore, goal.getCurrentStreak()
        );

        return Math.min(100.0, momentumScore);
    }

    private double applyStreakMultiplier(
            double score, Integer streak) {
        if (streak == null || streak == 0) {
            return score * 0.90; // penalty for no streak
        }
        double multiplier;
        if (streak >= 8) multiplier = 1.20;
        else if (streak >= 4) multiplier = 1.10;
        else if (streak >= 2) multiplier = 1.05;
        else multiplier = 1.00;

        return score * multiplier;
    }

    // =========================================================
    // PROGRESS SCORE (TIME-PACED)
    // =========================================================

    private Double calculateProgressScore(Goal goal) {
        if (goal.getProgressPercentage() == null) return 0.0;

        // If no dates set, fall back to raw progress
        if (goal.getStartDate() == null 
                || goal.getTargetDate() == null) {
            return Math.min(100.0, goal.getProgressPercentage());
        }

        LocalDate start = goal.getStartDate().toLocalDate();
        LocalDate target = goal.getTargetDate().toLocalDate();
        LocalDate today = LocalDate.now();

        // Total duration in days
        long totalDays = java.time.temporal.ChronoUnit.DAYS
            .between(start, target);
        if (totalDays <= 0) {
            return Math.min(100.0, goal.getProgressPercentage());
        }

        // Days elapsed
        long daysElapsed = java.time.temporal.ChronoUnit.DAYS
            .between(start, today);
        daysElapsed = Math.max(0, Math.min(daysElapsed, totalDays));

        // Time elapsed ratio
        double timeElapsedRatio = (double) daysElapsed / totalDays;
        double expectedProgress = timeElapsedRatio * 100.0;

        // Actual progress
        double actualProgress = Math.min(
            100.0, goal.getProgressPercentage()
        );

        // If no time has elapsed yet, return raw progress
        if (expectedProgress == 0) {
            return actualProgress;
        }

        // Time-paced score:
        // Are you ahead or behind pace?
        double pacedScore = (actualProgress / expectedProgress) * 100.0;
        return Math.min(100.0, pacedScore);
    }

    // =========================================================
    // FINAL HEALTH SCORE
    // =========================================================

    private Double calculateHealthScore(
            Goal goal,
            Double consistencyScore,
            Double momentumScore,
            Double progressScore) {

        // If all scores are null, health is untracked
        if (consistencyScore == null 
                && momentumScore == null 
                && progressScore == null) {
            return null;
        }

        // Resolve effective weights
        int cw = goal.getEffectiveConsistencyWeight();
        int mw = goal.getEffectiveMomentumWeight();
        int pw = goal.getEffectiveProgressWeight();

        // Treat null scores as 0 in calculation
        double c = consistencyScore != null ? consistencyScore : 0.0;
        double m = momentumScore != null ? momentumScore : 0.0;
        double p = progressScore != null ? progressScore : 0.0;

        double health = (c * cw / 100.0)
                      + (m * mw / 100.0)
                      + (p * pw / 100.0);

        return Math.min(100.0, Math.max(0.0, health));
    }

    // =========================================================
    // HEALTH STATUS DERIVATION
    // =========================================================

    private HealthStatus deriveHealthStatus(Double healthScore) {
        if (healthScore == null) return HealthStatus.UNTRACKED;
        if (healthScore >= 80) return HealthStatus.THRIVING;
        if (healthScore >= 60) return HealthStatus.ON_TRACK;
        if (healthScore >= 40) return HealthStatus.AT_RISK;
        return HealthStatus.CRITICAL;
    }

    // =========================================================
    // STREAK CALCULATION
    // =========================================================

    private void updateStreak(Goal goal) {
        if (goal.getTargetFrequencyWeekly() == null
                || goal.getTargetFrequencyWeekly() == 0) {
            return; // no streak tracking without frequency target
        }

        // Get all snapshots ordered by week desc
        List<GoalWeeklySnapshot> snapshots =
            snapshotRepository
                .findByGoalIdOrderByWeekStartDesc(goal.getId());

        if (snapshots.isEmpty()) {
            goal.setCurrentStreak(0);
            return;
        }

        int streak = 0;
        LocalDate expectedWeek = WeekUtils.getCurrentWeekMonday();
        int missesAllowed = goal.getDefaultMissesAllowedPerWeek();
        int missesUsed = 0;

        for (GoalWeeklySnapshot snapshot : snapshots) {
            // Check if this snapshot is for the expected week
            if (!snapshot.getWeekStart().equals(expectedWeek)) {
                // Gap week — counts as a miss
                missesUsed++;
                if (missesUsed > missesAllowed) break;
            }

            // Did they hit the target this week?
            boolean hitTarget = snapshot.getActivitiesLogged()
                >= goal.getTargetFrequencyWeekly();

            if (hitTarget) {
                streak++;
                expectedWeek = expectedWeek.minusWeeks(1);
            } else {
                missesUsed++;
                if (missesUsed > missesAllowed) break;
                expectedWeek = expectedWeek.minusWeeks(1);
            }
        }

        goal.setCurrentStreak(streak);

        // Update longest streak if current is better
        if (goal.getLongestStreak() == null
                || streak > goal.getLongestStreak()) {
            goal.setLongestStreak(streak);
        }
    }
}
