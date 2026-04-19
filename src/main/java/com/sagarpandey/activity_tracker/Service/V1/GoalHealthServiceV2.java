package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Service.Interface.GoalHealthService;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Repository.ActivityRepository;
import com.sagarpandey.activity_tracker.models.Activity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoalHealthServiceV2 implements GoalHealthService {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private GoalPeriodRepository goalPeriodRepository;
    
    @Autowired
    private ActivityRepository activityRepository;
    
    // ============================================
    // MASTER GOAL LEVEL MATH (The Dashboard View)
    // ============================================
    
    @Override
    public Double calculateOverallHealthScore(Goal goal) {
        List<GoalPeriod> allPeriods = fetchSortedPeriods(goal.getUuid());
        if (allPeriods.isEmpty()) return null;
        return allPeriods.stream().mapToDouble(this::calculatePeriodOverallHealthScore).average().orElse(0.0);
    }
    
    @Override
    public Double calculateConsistencyScore(Goal goal) {
        List<GoalPeriod> allPeriods = fetchSortedPeriods(goal.getUuid());
        if (allPeriods.isEmpty()) return null;
        return allPeriods.stream().mapToDouble(this::calculatePeriodConsistencyScore).average().orElse(0.0);
    }
    
    @Override
    public Double calculateMomentumScore(Goal goal) {
        List<GoalPeriod> allPeriods = fetchSortedPeriods(goal.getUuid());
        if (allPeriods.isEmpty()) return null;
        return allPeriods.stream().mapToDouble(this::calculatePeriodMomentumScore).average().orElse(0.0);
    }
    
    @Override
    public Double calculateProgressScore(Goal goal) {
        List<GoalPeriod> allPeriods = fetchSortedPeriods(goal.getUuid());
        if (allPeriods.isEmpty()) return null;
        return allPeriods.stream().mapToDouble(this::calculatePeriodProgressScore).average().orElse(0.0);
    }

    @Override
    public void updateGoalHealth(Goal goal) {
        List<GoalPeriod> periods = fetchSortedPeriods(goal.getUuid());
        
        // Sync real activity data into periods first
        syncActivitiesToPeriods(goal, periods);
        
        // Save computed health scores down into the Period table
        for (GoalPeriod p : periods) {
            updateGoalPeriodHealth(p); 
        }
        
        // Save the rollup averages into the Goal table
        goal.setConsistencyScore(calculateConsistencyScore(goal));
        goal.setMomentumScore(calculateMomentumScore(goal));
        goal.setProgressPercentage(calculateProgressScore(goal));
        goal.setHealthScore(calculateOverallHealthScore(goal));
        
        updateStreak(goal, periods);
        goalRepository.save(goal);
    }

    private void syncActivitiesToPeriods(Goal goal, List<GoalPeriod> periods) {
        for (GoalPeriod period : periods) {
            // Define time window for this period
            OffsetDateTime start = period.getPeriodStart().atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = period.getPeriodEnd().plusDays(1).atStartOfDay().minusNanos(1).atOffset(ZoneOffset.UTC);
            
            // Fetch all activities in this window
            List<Activity> activities = activityRepository.findByStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndUserId(
                start, end, goal.getUserId()
            );
            
            // Filter by goalId
            List<Activity> goalActivities = activities.stream()
                .filter(a -> goal.getId().equals(a.getGoalId()))
                .collect(Collectors.toList());
            
            // Update currentValue based on metric
            if (goal.getMetric() == Goal.Metric.DURATION) {
                double totalMinutes = goalActivities.stream()
                    .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                    .mapToDouble(a -> Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                    .sum();
                period.setCurrentValue(totalMinutes);
            } else {
                // Default to COUNT
                period.setCurrentValue((double) goalActivities.size());
            }
            
            goalPeriodRepository.save(period);
        }
    }

    // ============================================
    // GOAL PERIOD LEVEL MATH (The Brain)
    // ============================================
    
    @Override
    public Double calculatePeriodConsistencyScore(GoalPeriod period) {
        if (period.getMinimumSessionPeriod() == null || period.getMinimumSessionPeriod() <= 0) return 0.0;
        Double current = period.getCurrentValue() != null ? period.getCurrentValue() : 0.0;
        return Math.min(100.0, (current / period.getMinimumSessionPeriod()) * 100.0);
    }
    
    @Override
    public Double calculatePeriodProgressScore(GoalPeriod period) {
        if (period.getMaximumSessionPeriod() == null || period.getMaximumSessionPeriod() <= 0) return 0.0;
        Double current = period.getCurrentValue() != null ? period.getCurrentValue() : 0.0;
        return Math.min(100.0, (current / period.getMaximumSessionPeriod()) * 100.0);
    }
    
    @Override
    public Double calculatePeriodMomentumScore(GoalPeriod period) {
        List<GoalPeriod> allHistorical = fetchSortedPeriods(period.getParentGoalUuid());
        int currentIndex = allHistorical.indexOf(period);
        
        // Safety grab by UUID if object mapping differs
        if (currentIndex == -1) {
            for (int i = 0; i < allHistorical.size(); i++) {
                if (allHistorical.get(i).getUuid().equals(period.getUuid())) {
                    currentIndex = i; break;
                }
            }
        }
        
        if (currentIndex < 2) {
            Double curCon = calculatePeriodConsistencyScore(period);
            Double curProg = calculatePeriodProgressScore(period);
            return (curCon + curProg) / 2.0; 
        }
        
        GoalPeriod prev1 = allHistorical.get(currentIndex - 1);
        GoalPeriod prev2 = allHistorical.get(currentIndex - 2);
        
        double currentAvg = (calculatePeriodConsistencyScore(period) + calculatePeriodProgressScore(period)) / 2.0;
        double pastAvg1 = (calculatePeriodConsistencyScore(prev1) + calculatePeriodProgressScore(prev1)) / 2.0;
        double pastAvg2 = (calculatePeriodConsistencyScore(prev2) + calculatePeriodProgressScore(prev2)) / 2.0;
        
        double targetToBeat = (pastAvg1 + pastAvg2) / 2.0;
        
        if (targetToBeat <= 0) {
            return currentAvg > 0 ? 100.0 : 0.0;
        }
        
        if (currentAvg >= targetToBeat) {
            return 100.0; 
        }
        
        return Math.min(100.0, (currentAvg / targetToBeat) * 100.0);
    }
    
    @Override
    public Double calculatePeriodOverallHealthScore(GoalPeriod period) {
        // First get the parent goal to access userId
        Optional<Goal> parentGoalOpt = goalRepository.findByUuidAndUserIdAndIsDeletedFalse(period.getParentGoalUuid(), null);
        if (parentGoalOpt.isEmpty()) return 0.0;
        Goal parentGoal = parentGoalOpt.get();
        
        int cw = (parentGoal != null && parentGoal.getConsistencyWeight() != null) ? parentGoal.getConsistencyWeight() : 33;
        int mw = (parentGoal != null && parentGoal.getMomentumWeight() != null) ? parentGoal.getMomentumWeight() : 33;
        int pw = (parentGoal != null && parentGoal.getProgressWeight() != null) ? parentGoal.getProgressWeight() : 34;
        
        double c = calculatePeriodConsistencyScore(period);
        double m = calculatePeriodMomentumScore(period);
        double p = calculatePeriodProgressScore(period);
        
        double sumProduct = (c * cw) + (m * mw) + (p * pw);
        int totalWeight = cw + mw + pw;
        
        if (totalWeight <= 0) return 0.0;
        return Math.min(100.0, sumProduct / totalWeight);
    }
    
    @Override
    public void updateGoalPeriodHealth(GoalPeriod period) {
        period.setConsistencyScore(calculatePeriodConsistencyScore(period));
        period.setMomentumScore(calculatePeriodMomentumScore(period));
        period.setProgressScore(calculatePeriodProgressScore(period));
        period.setProgressPercentage(calculatePeriodProgressScore(period)); // Keep both for now
        period.setHealthScore(calculatePeriodOverallHealthScore(period));
        goalPeriodRepository.save(period);
    }
    
    // ============================================
    // UTILS & DB SYNC
    // ============================================
    
    private List<GoalPeriod> fetchSortedPeriods(String parentGoalUuid) {
        if(parentGoalUuid == null) return new ArrayList<>();
        List<GoalPeriod> periods = goalPeriodRepository.findByParentGoalUuid(parentGoalUuid);
        periods.sort(Comparator.comparing(GoalPeriod::getPeriodStart));
        return periods;
    }
    
    @Override
    public Map<String, Object> getHealthBreakdown(Goal goal) {
        Map<String, Object> bd = new HashMap<>();
        bd.put("goalUuid", goal.getUuid());
        bd.put("overallHealthScore", calculateOverallHealthScore(goal));
        bd.put("consistencyScore", calculateConsistencyScore(goal));
        bd.put("momentumScore", calculateMomentumScore(goal));
        bd.put("progressScore", calculateProgressScore(goal));
        bd.put("currentStreak", goal.getCurrentStreak());
        return bd;
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
        
        Double avgOverall = allGoals.stream().mapToDouble(g -> calculateOverallHealthScore(g) != null ? calculateOverallHealthScore(g) : 0.0).average().orElse(0.0);
        stats.put("averageOverallHealthScore", avgOverall);
        return stats;
    }

    @Override
    public boolean needsHealthUpdate(Goal goal) {
        if (goal.getLastUpdatedAt() == null) return true;
        return goal.getLastUpdatedAt().isBefore(LocalDateTime.now().minusHours(1));
    }
    
    private void updateStreak(Goal goal, List<GoalPeriod> sortedPeriods) {
        if (sortedPeriods.isEmpty()) {
            goal.setCurrentStreak(0);
            return;
        }
        
        int streak = 0;
        int missesAllowed = goal.getMissesAllowedPerPeriod() != null ? goal.getMissesAllowedPerPeriod() : 0;
        int missesUsed = 0;
        
        for (GoalPeriod p : sortedPeriods) {
            boolean hit = calculatePeriodConsistencyScore(p) >= 100.0;
            if (hit) {
                streak++;
            } else {
                missesUsed++;
                if (missesUsed > missesAllowed) {
                    streak = 0;
                    missesUsed = 0;
                }
            }
        }
        
        goal.setCurrentStreak(streak);
        if (goal.getLongestStreak() == null || streak > goal.getLongestStreak()) {
            goal.setLongestStreak(streak);
        }
    }
}
