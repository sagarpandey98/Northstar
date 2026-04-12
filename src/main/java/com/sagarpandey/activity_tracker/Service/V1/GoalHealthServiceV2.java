package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Service.Interface.GoalHealthService;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalWeeklySnapshot;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Repository.GoalWeeklySnapshotRepository;
import com.sagarpandey.activity_tracker.utils.WeekUtils;
import com.sagarpandey.activity_tracker.utils.PeriodUtils;
import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of GoalHealthService providing centralized health calculation logic
 * Refactored from existing GoalHealthServiceV1 to provide clean service layer
 */
@Service
@Transactional
public class GoalHealthServiceV2 implements GoalHealthService {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private GoalWeeklySnapshotRepository snapshotRepository;
    
    @Override
    public Double calculateOverallHealthScore(Goal goal) {
        // Get effective weights for this goal
        int consistencyWeight = goal.getEffectiveConsistencyWeight();
        int momentumWeight = goal.getEffectiveMomentumWeight();
        int progressWeight = goal.getEffectiveProgressWeight();
        
        // Calculate individual scores
        Double consistencyScore = calculateConsistencyScore(goal);
        Double momentumScore = calculateMomentumScore(goal);
        Double progressScore = calculateProgressScore(goal);
        
        // Handle untracked goals
        if (consistencyScore == null && momentumScore == null && progressScore == null) {
            return null;
        }
        
        // Calculate weighted average
        double overallScore = 0.0;
        int totalWeight = 0;
        
        if (consistencyScore != null) {
            overallScore += consistencyScore * consistencyWeight;
            totalWeight += consistencyWeight;
        }
        
        if (momentumScore != null) {
            overallScore += momentumScore * momentumWeight;
            totalWeight += momentumWeight;
        }
        
        if (progressScore != null) {
            overallScore += progressScore * progressWeight;
            totalWeight += progressWeight;
        }
        
        if (totalWeight == 0) {
            return null;
        }
        
        return Math.min(100.0, overallScore / totalWeight);
    }
    
    @Override
    public Double calculateConsistencyScore(Goal goal) {
        // Get target for this goal's period
        Integer targetPerPeriod = getTargetForPeriod(goal);
        if (targetPerPeriod == null || targetPerPeriod == 0) {
            return null; // untracked
        }
        
        if (goal.getEvaluationPeriod() != null && goal.getEvaluationPeriod() != EvaluationPeriod.WEEKLY) {
            // Period-aware consistency calculation
            return calculatePeriodConsistency(goal, targetPerPeriod);
        } else {
            // Weekly consistency calculation (existing logic)
            return calculateWeeklyConsistency(goal, targetPerPeriod);
        }
    }
    
    @Override
    public Double calculateMomentumScore(Goal goal) {
        // Get target for this goal's period
        Integer targetPerPeriod = getTargetForPeriod(goal);
        if (targetPerPeriod == null || targetPerPeriod == 0) {
            return null; // untracked
        }
        
        // Get last 4 periods of snapshots based on evaluation period
        List<Double> periodScores = getLastPeriodScores(goal, 4);
        
        // Weighted rolling average [period-3, period-2, period-1, current]
        // with weights                   [0.10,   0.20,   0.30,  0.40]
        double[] weights = {0.10, 0.20, 0.30, 0.40};
        double momentumScore = 0.0;
        
        for (int i = 0; i < periodScores.size(); i++) {
            momentumScore += periodScores.get(i) * weights[i];
        }
        
        // Apply streak multiplier
        momentumScore = applyStreakMultiplier(
            momentumScore, goal.getCurrentStreak()
        );
        
        return Math.min(100.0, momentumScore);
    }
    
    @Override
    public Double calculateProgressScore(Goal goal) {
        if (goal.getTargetValue() == null || goal.getTargetValue() == 0.0) {
            return null; // untracked
        }
        
        double progress = goal.getProgressPercentage() != null ? 
            goal.getProgressPercentage() : 0.0;
        
        return Math.min(100.0, Math.max(0.0, progress));
    }
    
    @Override
    public void updateGoalHealth(Goal goal) {
        // Update all health scores
        Double consistencyScore = calculateConsistencyScore(goal);
        Double momentumScore = calculateMomentumScore(goal);
        Double progressScore = calculateProgressScore(goal);
        Double overallScore = calculateOverallHealthScore(goal);
        
        // Set scores on goal
        goal.setConsistencyScore(consistencyScore);
        goal.setMomentumScore(momentumScore);
        goal.setHealthScore(overallScore);
        
        // Update streak
        updateStreak(goal);
        
        // Save goal
        goalRepository.save(goal);
    }
    
    @Override
    public Map<String, Object> getHealthBreakdown(Goal goal) {
        Map<String, Object> breakdown = new HashMap<>();
        
        breakdown.put("goalUuid", goal.getUuid());
        breakdown.put("goalTitle", goal.getTitle());
        breakdown.put("overallHealthScore", calculateOverallHealthScore(goal));
        breakdown.put("consistencyScore", calculateConsistencyScore(goal));
        breakdown.put("momentumScore", calculateMomentumScore(goal));
        breakdown.put("progressScore", calculateProgressScore(goal));
        
        // Add weight information
        breakdown.put("consistencyWeight", goal.getEffectiveConsistencyWeight());
        breakdown.put("momentumWeight", goal.getEffectiveMomentumWeight());
        breakdown.put("progressWeight", goal.getEffectiveProgressWeight());
        
        // Add streak information
        breakdown.put("currentStreak", goal.getCurrentStreak());
        breakdown.put("longestStreak", goal.getLongestStreak());
        
        // Add period information
        breakdown.put("evaluationPeriod", goal.getEvaluationPeriod());
        breakdown.put("targetPerPeriod", getTargetForPeriod(goal));
        
        return breakdown;
    }
    
    @Override
    public Map<String, Double> calculateHealthScores(List<Goal> goals) {
        return goals.stream()
            .collect(Collectors.toMap(
                Goal::getUuid,
                this::calculateOverallHealthScore
            ));
    }
    
    @Override
    public Map<String, Object> getHealthStatistics(String userId) {
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGoals", allGoals.size());
        
        // Calculate average scores
        Double avgOverall = allGoals.stream()
            .mapToDouble(goal -> calculateOverallHealthScore(goal) != null ? 
                calculateOverallHealthScore(goal) : 0.0)
            .average()
            .orElse(0.0);
        
        Double avgConsistency = allGoals.stream()
            .mapToDouble(goal -> calculateConsistencyScore(goal) != null ? 
                calculateConsistencyScore(goal) : 0.0)
            .average()
            .orElse(0.0);
        
        Double avgMomentum = allGoals.stream()
            .mapToDouble(goal -> calculateMomentumScore(goal) != null ? 
                calculateMomentumScore(goal) : 0.0)
            .average()
            .orElse(0.0);
        
        Double avgProgress = allGoals.stream()
            .mapToDouble(goal -> calculateProgressScore(goal) != null ? 
                calculateProgressScore(goal) : 0.0)
            .average()
            .orElse(0.0);
        
        stats.put("averageOverallHealthScore", avgOverall);
        stats.put("averageConsistencyScore", avgConsistency);
        stats.put("averageMomentumScore", avgMomentum);
        stats.put("averageProgressScore", avgProgress);
        
        // Health distribution
        Map<String, Long> healthDistribution = allGoals.stream()
            .filter(goal -> calculateOverallHealthScore(goal) != null)
            .collect(Collectors.groupingBy(
                goal -> {
                    Double score = calculateOverallHealthScore(goal);
                    if (score >= 80) return "EXCELLENT";
                    if (score >= 60) return "GOOD";
                    if (score >= 40) return "FAIR";
                    return "POOR";
                },
                Collectors.counting()
            ));
        
        stats.put("healthDistribution", healthDistribution);
        
        return stats;
    }
    
    @Override
    public boolean needsHealthUpdate(Goal goal) {
        // Check if last health update was more than 1 hour ago
        if (goal.getLastUpdatedAt() == null) {
            return true;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        return goal.getLastUpdatedAt().isBefore(cutoff);
    }
    
    // Helper methods
    
    private Integer getTargetForPeriod(Goal goal) {
        if (goal.getEvaluationPeriod() != null) {
            return goal.getTargetPerPeriod();
        }
        // Fallback to old weekly system
        return goal.getTargetFrequencyWeekly();
    }
    
    private Double calculateWeeklyConsistency(Goal goal, Integer targetFrequency) {
        LocalDate weekStart = WeekUtils.getCurrentWeekMonday();
        Optional<GoalWeeklySnapshot> currentWeekSnapshot =
            snapshotRepository.findByGoalIdAndWeekStart(
                goal.getId(), weekStart
            );
        
        int activitiesThisWeek = currentWeekSnapshot
            .map(GoalWeeklySnapshot::getActivitiesLogged)
            .orElse(0);
        
        // Time-paced consistency:
        // Expected by now = target * (dayOfWeek / 7)
        // Monday=1, Sunday=7
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        double expectedByNow = targetFrequency * (dayOfWeek / 7.0);
        
        // Avoid division by zero on Monday before any activity
        if (expectedByNow == 0) expectedByNow = 1;
        
        double score = (activitiesThisWeek / expectedByNow) * 100.0;
        return Math.min(100.0, score);
    }
    
    private Double calculatePeriodConsistency(Goal goal, Integer targetPerPeriod) {
        // Implementation for period-aware consistency calculation
        // This would use PeriodSnapshot data
        // For now, return a placeholder
        return 75.0; // Placeholder implementation
    }
    
    private List<Double> getLastPeriodScores(Goal goal, int periodCount) {
        List<Double> scores = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        
        if (goal.getEvaluationPeriod() != null && goal.getEvaluationPeriod() != EvaluationPeriod.WEEKLY) {
            // Use period snapshots for non-weekly goals
            LocalDate periodStart = PeriodUtils.getPeriodStart(
                currentDate, goal.getEvaluationPeriod(), 
                goal.getCustomPeriodDays(), goal.getCurrentPeriodStart()
            );
            
            for (int i = 0; i < periodCount; i++) {
                LocalDate targetPeriodStart = periodStart.minusDays(i * getPeriodLength(goal));
                Double score = getPeriodSnapshotScore(goal, targetPeriodStart);
                scores.add(score != null ? score : 0.0);
            }
        } else {
            // Use weekly snapshots for weekly goals (existing logic)
            for (int i = 0; i < periodCount; i++) {
                LocalDate weekStart = WeekUtils.weeksAgo(i);
                Double score = getWeeklySnapshotScore(goal, weekStart);
                scores.add(score != null ? score : 0.0);
            }
        }
        
        return scores.reversed(); // [period-3, period-2, period-1, current]
    }
    
    private int getPeriodLength(Goal goal) {
        if (goal.getEvaluationPeriod() == null) return 7; // weekly fallback
        
        return switch (goal.getEvaluationPeriod()) {
            case DAILY -> 1;
            case WEEKLY -> 7;
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case YEARLY -> 365;
            case CUSTOM -> goal.getCustomPeriodDays() != null ? goal.getCustomPeriodDays() : 7;
        };
    }
    
    private Double getPeriodSnapshotScore(Goal goal, LocalDate periodStart) {
        // Implementation would fetch from GoalPeriodSnapshot table
        // For now, return null to be implemented
        return null;
    }
    
    private Double getWeeklySnapshotScore(Goal goal, LocalDate weekStart) {
        Optional<GoalWeeklySnapshot> snapshot = snapshotRepository.findByGoalIdAndWeekStart(
            goal.getId(), weekStart
        );
        
        return snapshot.map(s -> {
            if (s.getConsistencyScoreForWeek() != null) {
                return s.getConsistencyScoreForWeek();
            }
            return WeekUtils.calculateConsistencyScore(
                s.getActivitiesLogged(),
                goal.getTargetFrequencyWeekly()
            );
        }).orElse(0.0);
    }
    
    private double applyStreakMultiplier(double score, Integer streak) {
        if (streak == null || streak <= 0) {
            return score * 0.9; // 10% penalty for no streak
        }
        
        if (streak >= 4) {
            return score * 1.1; // 10% bonus for 4+ week streak
        }
        
        return score; // no adjustment for 1-3 week streak
    }
    
    private void updateStreak(Goal goal) {
        // Get target for this goal's period
        Integer targetPerPeriod = getTargetForPeriod(goal);
        if (targetPerPeriod == null || targetPerPeriod == 0) {
            return; // no streak tracking without frequency target
        }
        
        // Get last periods based on evaluation period
        List<Double> periodScores = getLastPeriodScores(goal, 20); // Get more periods for streak calculation
        
        if (periodScores.isEmpty()) {
            goal.setCurrentStreak(0);
            return;
        }
        
        int streak = 0;
        int missesAllowed = goal.getDefaultMissesAllowedPerPeriod();
        int missesUsed = 0;
        
        // Calculate streak from period scores
        for (Double score : periodScores) {
            // Did they hit the target this period?
            boolean hitTarget = score >= 80.0; // 80% consistency = hit target
            
            if (hitTarget) {
                streak++;
            } else {
                missesUsed++;
                if (missesUsed > missesAllowed) break;
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
