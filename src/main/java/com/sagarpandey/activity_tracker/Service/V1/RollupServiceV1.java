package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.Service.Interface.RollupService;
import com.sagarpandey.activity_tracker.dtos.ParentInsights;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.utils.WeekUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RollupServiceV1 implements RollupService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalPeriodRepository periodRepository;

    // =========================================================
    // LEAF CHECK
    // =========================================================

    @Override
    public boolean isLeafGoal(String goalUuid, String userId) {
        List<Goal> children = goalRepository
            .findByParentGoalIdAndUserIdAndIsDeletedFalse(
                goalUuid, userId
            );
        return children.isEmpty();
    }

    // =========================================================
    // ROLLED UP HEALTH SCORE
    // Priority weights:
    // CRITICAL=4, HIGH=3, MEDIUM=2, LOW=1
    // =========================================================

    @Override
    public Double calculateRolledUpHealthScore(
            List<Goal> children) {
        if (children == null || children.isEmpty()) return null;

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Goal child : children) {
            if (child.getHealthScore() == null) continue;

            double weight = getPriorityWeight(child.getPriority());
            weightedSum += child.getHealthScore() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) return null;
        return Math.min(100.0, weightedSum / totalWeight);
    }

    private double getPriorityWeight(Goal.Priority priority) {
        if (priority == null) return 1.0;
        return switch (priority) {
            case CRITICAL -> 4.0;
            case HIGH -> 3.0;
            case MEDIUM -> 2.0;
            case LOW -> 1.0;
        };
    }

    // =========================================================
    // PARENT INSIGHTS
    // =========================================================

    @Override
    public ParentInsights buildParentInsights(
            Long goalId, String userId) {

        // Get the goal to find its uuid
        Goal parentGoal = goalRepository.findById(goalId)
            .orElse(null);
        if (parentGoal == null) return null;

        // Get direct children
        List<Goal> children = goalRepository
            .findByParentGoalIdAndUserIdAndIsDeletedFalse(
                parentGoal.getUuid(), userId
            );

        if (children.isEmpty()) return null;

        ParentInsights insights = new ParentInsights();

        // --- Children Summary ---
        ParentInsights.ChildrenSummary summary =
            new ParentInsights.ChildrenSummary();
        summary.setTotal(children.size());

        int thriving = 0, onTrack = 0, atRisk = 0,
            critical = 0, untracked = 0;

        Goal weakest = null;

        for (Goal child : children) {
            HealthStatus status = deriveStatus(child.getHealthScore());
            switch (status) {
                case THRIVING -> thriving++;
                case ON_TRACK -> onTrack++;
                case AT_RISK -> atRisk++;
                case CRITICAL -> critical++;
                case UNTRACKED -> untracked++;
            }

            // Track weakest child (lowest healthScore)
            if (child.getHealthScore() != null) {
                if (weakest == null
                        || child.getHealthScore()
                            < weakest.getHealthScore()) {
                    weakest = child;
                }
            }
        }

        summary.setThriving(thriving);
        summary.setOnTrack(onTrack);
        summary.setAtRisk(atRisk);
        summary.setCritical(critical);
        summary.setUntracked(untracked);
        insights.setChildrenSummary(summary);

        // --- Weakest Child ---
        if (weakest != null) {
            ParentInsights.WeakestChild weakestChild =
                new ParentInsights.WeakestChild();
            weakestChild.setId(weakest.getId());
            weakestChild.setTitle(weakest.getTitle());
            weakestChild.setHealthScore(weakest.getHealthScore());
            weakestChild.setHealthStatus(
                deriveStatus(weakest.getHealthScore())
            );
            insights.setWeakestChild(weakestChild);
        }

        // --- Completion Velocity ---
        ParentInsights.CompletionVelocity velocity =
            new ParentInsights.CompletionVelocity();
        int cvOnTrack = 0, slipping = 0, completed = 0;

        for (Goal child : children) {
            if (child.getStatus() == Goal.Status.COMPLETED) {
                completed++;
            } else if (child.getStatus() == Goal.Status.OVERDUE) {
                slipping++;
            } else if (child.getTargetDate() != null) {
                // Check if behind pace using progress score
                // If progressPercentage < timeElapsedRatio*100
                // then slipping
                boolean isBehind = isChildSlipping(child);
                if (isBehind) slipping++;
                else cvOnTrack++;
            } else {
                cvOnTrack++;
            }
        }

        velocity.setOnTrack(cvOnTrack);
        velocity.setSlipping(slipping);
        velocity.setCompleted(completed);
        insights.setCompletionVelocity(velocity);

        // --- Health Score Last Week ---
        // Get the parent goal's health score from last week
        // by looking at snapshots from last week's children
        // Simplified: use rolled-up score from last week's
        // snapshots
        Double healthScoreLastWeek =
            calculateLastWeekHealthScore(children);
        insights.setHealthScoreLastWeek(healthScoreLastWeek);

        return insights;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private HealthStatus deriveStatus(Double healthScore) {
        if (healthScore == null) return HealthStatus.UNTRACKED;
        if (healthScore >= 80) return HealthStatus.THRIVING;
        if (healthScore >= 60) return HealthStatus.ON_TRACK;
        if (healthScore >= 40) return HealthStatus.AT_RISK;
        return HealthStatus.CRITICAL;
    }

    private boolean isChildSlipping(Goal child) {
        if (child.getStartDate() == null
                || child.getTargetDate() == null) {
            return false;
        }

        long totalDays = java.time.temporal.ChronoUnit.DAYS
            .between(
                child.getStartDate().toLocalDate(),
                child.getTargetDate().toLocalDate()
            );
        if (totalDays <= 0) return false;

        long daysElapsed = java.time.temporal.ChronoUnit.DAYS
            .between(
                child.getStartDate().toLocalDate(),
                LocalDate.now()
            );
        daysElapsed = Math.max(0,
            Math.min(daysElapsed, totalDays));

        double timeElapsedRatio =
            (double) daysElapsed / totalDays;
        double expectedProgress = timeElapsedRatio * 100.0;

        double actualProgress = child.getProgressPercentage()
            != null ? child.getProgressPercentage() : 0.0;

        return actualProgress < expectedProgress * 0.8;
    }

    private Double calculateLastWeekHealthScore(
            List<Goal> children) {
        LocalDate lastWeek = LocalDate.now().minusDays(7);

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Goal child : children) {
            List<GoalPeriod> periods = periodRepository.findByParentGoalUuid(child.getUuid());
            Optional<GoalPeriod> lastWeekPeriod = periods.stream()
                .filter(p -> p.getPeriodStart() != null && !p.getPeriodStart().isAfter(lastWeek))
                .max(java.util.Comparator.comparing(GoalPeriod::getPeriodStart));

            if (lastWeekPeriod.isPresent()
                    && lastWeekPeriod.get().getConsistencyScore() != null) {
                double weight = getPriorityWeight(child.getPriority());
                weightedSum += lastWeekPeriod.get().getConsistencyScore() * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) return null;
        return Math.min(100.0, weightedSum / totalWeight);
    }
}
