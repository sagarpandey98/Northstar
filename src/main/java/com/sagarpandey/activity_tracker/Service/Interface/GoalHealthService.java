package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;

import java.util.List;
import java.util.Map;

/**
 * GoalHealthService interface for calculating goal health scores
 * 
 * NOTE ON NEW ARCHITECTURE (Time-Bounded Ledger System):
 * In the refactored architecture, all core math computations (Progress, Consistency, 
 * Momentum) happen strictly at the 'GoalPeriod' level based on minimum/maximum boundaries.
 * This Service Interface exposes BOTH the Goal and GoalPeriod components so clients
 * can manually fetch stats for any period independently at any time.
 */
public interface GoalHealthService {
    
    // ============================================
    // MASTER GOAL DASHBOARD CALCULATIONS
    // ============================================
    Double calculateOverallHealthScore(Goal goal);
    Double calculateConsistencyScore(Goal goal);
    Double calculateMomentumScore(Goal goal);
    Double calculateProgressScore(Goal goal);
    void updateGoalHealth(Goal goal);
    
    Map<String, Object> getHealthBreakdown(Goal goal);
    Map<String, Double> calculateHealthScores(List<Goal> goals);
    Map<String, Object> getHealthStatistics(String userId);
    boolean needsHealthUpdate(Goal goal);
    
    // ============================================
    // GOAL PERIOD CORE CALCULATIONS (The Brain)
    // ============================================
    Double calculatePeriodOverallHealthScore(GoalPeriod period);
    Double calculatePeriodConsistencyScore(GoalPeriod period);
    Double calculatePeriodMomentumScore(GoalPeriod period);
    Double calculatePeriodProgressScore(GoalPeriod period);
    void updateGoalPeriodHealth(GoalPeriod period);
}
