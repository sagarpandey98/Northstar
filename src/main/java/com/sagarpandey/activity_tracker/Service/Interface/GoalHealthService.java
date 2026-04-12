package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.models.Goal;

import java.util.List;
import java.util.Map;

/**
 * GoalHealthService interface for calculating goal health scores
 * Provides centralized health calculation logic that can be used anywhere in the application
 */
public interface GoalHealthService {
    
    /**
     * Calculate overall health score for a goal
     * Combines consistency, momentum, and progress scores
     * 
     * @param goal goal to calculate health for
     * @return overall health score (0-100)
     */
    Double calculateOverallHealthScore(Goal goal);
    
    /**
     * Calculate consistency score for a goal
     * Measures how consistently the user is meeting their goals
     * 
     * @param goal goal to calculate consistency for
     * @return consistency score (0-100)
     */
    Double calculateConsistencyScore(Goal goal);
    
    /**
     * Calculate momentum score for a goal
     * Measures recent performance trends
     * 
     * @param goal goal to calculate momentum for
     * @return momentum score (0-100)
     */
    Double calculateMomentumScore(Goal goal);
    
    /**
     * Calculate progress score for a goal
     * Measures completion progress
     * 
     * @param goal goal to calculate progress for
     * @return progress score (0-100)
     */
    Double calculateProgressScore(Goal goal);
    
    /**
     * Update all health scores for a goal
     * Recalculates and saves all health-related metrics
     * 
     * @param goal goal to update health for
     */
    void updateGoalHealth(Goal goal);
    
    /**
     * Get health score breakdown for a goal
     * Returns individual component scores and overall score
     * 
     * @param goal goal to get health breakdown for
     * @return map of health components and their scores
     */
    Map<String, Object> getHealthBreakdown(Goal goal);
    
    /**
     * Calculate health scores for multiple goals
     * Batch processing for efficiency
     * 
     * @param goals list of goals to calculate health for
     * @return map of goal UUID to health score
     */
    Map<String, Double> calculateHealthScores(List<Goal> goals);
    
    /**
     * Get health statistics for user's goals
     * Provides aggregate health metrics
     * 
     * @param userId user ID
     * @return health statistics
     */
    Map<String, Object> getHealthStatistics(String userId);
    
    /**
     * Check if goal needs health update
     * Determines if health recalculation is needed
     * 
     * @param goal goal to check
     * @return true if health update is needed
     */
    boolean needsHealthUpdate(Goal goal);
}
