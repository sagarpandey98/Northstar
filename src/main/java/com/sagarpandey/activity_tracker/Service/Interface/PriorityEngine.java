package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.models.Goal;
import java.util.List;
import java.util.Map;

/**
 * PriorityEngine interface for calculating and managing goal priorities
 * across hierarchical goal structures with parent-child relationships.
 */
public interface PriorityEngine {
    
    /**
     * Calculate the effective priority score for a goal
     * combining both parent and child priorities
     * 
     * @param goal the goal to calculate priority for
     * @return numerical priority score (higher = more important)
     */
    double calculateEffectivePriorityScore(Goal goal);
    
    /**
     * Calculate the effective priority score for a goal with parent context
     * 
     * @param goal the goal to calculate priority for
     * @param parentGoal the parent goal (can be null for root goals)
     * @return numerical priority score (higher = more important)
     */
    double calculateEffectivePriorityScore(Goal goal, Goal parentGoal);
    
    /**
     * Get all goals sorted by effective priority (highest first)
     * 
     * @param goals list of goals to sort
     * @return goals sorted by effective priority score
     */
    List<Goal> getGoalsSortedByPriority(List<Goal> goals);
    
    /**
     * Group goals by priority level
     * 
     * @param goals list of goals to group
     * @return map of priority level to list of goals
     */
    Map<Goal.Priority, List<Goal>> getGoalsGroupedByPriority(List<Goal> goals);
    
    /**
     * Get Today's Focus list - most important goals across all hierarchies
     * 
     * @param goals list of all user goals
     * @param limit maximum number of goals to return
     * @return prioritized list of goals for today's focus
     */
    List<Goal> getTodaysFocus(List<Goal> goals, int limit);
    
    /**
     * Get numerical value for priority enum
     * 
     * @param priority the priority enum
     * @return numerical value (CRITICAL=4, HIGH=3, MEDIUM=2, LOW=1)
     */
    int getPriorityValue(Goal.Priority priority);
    
    /**
     * Calculate weighted priority score combining parent and child priorities
     * 
     * @param childPriority child goal's priority
     * @param parentPriority parent goal's priority
     * @param childWeight weight for child priority (0.0-1.0)
     * @param parentWeight weight for parent priority (0.0-1.0)
     * @return combined priority score
     */
    double calculateWeightedPriorityScore(Goal.Priority childPriority, Goal.Priority parentPriority, double childWeight, double parentWeight);
}
