package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Interface.PriorityEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PriorityEngine for calculating effective priorities
 * across hierarchical goal structures.
 */
@Service
public class PriorityEngineV1 implements PriorityEngine {
    
    @Autowired
    private GoalRepository goalRepository;
    
    // Priority weights for combination logic
    private static final double CHILD_PRIORITY_WEIGHT = 0.7;
    private static final double PARENT_PRIORITY_WEIGHT = 0.3;
    
    @Override
    public double calculateEffectivePriorityScore(Goal goal) {
        Goal parentGoal = null;
        if (goal.getParentGoalId() != null) {
            Optional<Goal> parentOpt = goalRepository.findByUuidAndUserIdAndIsDeletedFalse(
                goal.getParentGoalId(), goal.getUserId()
            );
            parentGoal = parentOpt.orElse(null);
        }
        
        return calculateEffectivePriorityScore(goal, parentGoal);
    }
    
    @Override
    public double calculateEffectivePriorityScore(Goal goal, Goal parentGoal) {
        int childPriorityValue = getPriorityValue(goal.getPriority());
        
        if (parentGoal == null) {
            // Root goal - only child priority matters
            return childPriorityValue;
        }
        
        int parentPriorityValue = getPriorityValue(parentGoal.getPriority());
        
        // Weighted combination of parent and child priorities
        return calculateWeightedPriorityScore(
            goal.getPriority(), 
            parentGoal.getPriority(), 
            CHILD_PRIORITY_WEIGHT, 
            PARENT_PRIORITY_WEIGHT
        );
    }
    
    @Override
    public List<Goal> getGoalsSortedByPriority(List<Goal> goals) {
        return goals.stream()
            .sorted((g1, g2) -> Double.compare(
                calculateEffectivePriorityScore(g2), 
                calculateEffectivePriorityScore(g1)
            ))
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<Goal.Priority, List<Goal>> getGoalsGroupedByPriority(List<Goal> goals) {
        return goals.stream()
            .collect(Collectors.groupingBy(Goal::getPriority));
    }
    
    @Override
    public List<Goal> getTodaysFocus(List<Goal> goals, int limit) {
        return goals.stream()
            .sorted((g1, g2) -> Double.compare(
                calculateEffectivePriorityScore(g2), 
                calculateEffectivePriorityScore(g1)
            ))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public int getPriorityValue(Goal.Priority priority) {
        return switch (priority) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
    
    @Override
    public double calculateWeightedPriorityScore(Goal.Priority childPriority, Goal.Priority parentPriority, double childWeight, double parentWeight) {
        int childValue = getPriorityValue(childPriority);
        int parentValue = getPriorityValue(parentPriority);
        
        // Normalize weights to ensure they sum to 1
        double totalWeight = childWeight + parentWeight;
        double normalizedChildWeight = childWeight / totalWeight;
        double normalizedParentWeight = parentWeight / totalWeight;
        
        // Calculate weighted score
        double weightedScore = (childValue * normalizedChildWeight) + (parentValue * normalizedParentWeight);
        
        // Apply bonus for high-priority parents
        if (parentPriority == Goal.Priority.CRITICAL && childPriority != Goal.Priority.CRITICAL) {
            weightedScore += 0.5; // Bonus for children of critical goals
        }
        
        return weightedScore;
    }
    
    /**
     * Get all goals with their effective priority scores
     * 
     * @param userId the user ID
     * @return list of goals with priority scores
     */
    public List<GoalWithPriorityScore> getGoalsWithPriorityScores(String userId) {
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        
        return allGoals.stream()
            .map(goal -> new GoalWithPriorityScore(
                goal, 
                calculateEffectivePriorityScore(goal)
            ))
            .sorted((g1, g2) -> Double.compare(g2.getScore(), g1.getScore()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get Today's Focus list with priority scores
     * 
     * @param userId the user ID
     * @param limit maximum number of goals
     * @return list of goals with priority scores for today's focus
     */
    public List<GoalWithPriorityScore> getTodaysFocusWithScores(String userId, int limit) {
        List<GoalWithPriorityScore> goalsWithScores = getGoalsWithPriorityScores(userId);
        
        return goalsWithScores.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * DTO class for goal with priority score
     */
    public static class GoalWithPriorityScore {
        private final Goal goal;
        private final double score;
        
        public GoalWithPriorityScore(Goal goal, double score) {
            this.goal = goal;
            this.score = score;
        }
        
        public Goal getGoal() { return goal; }
        public double getScore() { return score; }
        
        @Override
        public String toString() {
            return String.format("%s (Priority: %.2f)", goal.getTitle(), score);
        }
    }
}
