package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.models.Goal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GoalStatusService interface for managing goal status transitions
 * Provides centralized status management logic that can be used anywhere in the application
 */
public interface GoalStatusService {
    
    /**
     * Determine the appropriate status for a goal based on its current state
     * 
     * @param goal goal to evaluate
     * @return calculated status
     */
    Goal.Status determineGoalStatus(Goal goal);
    
    /**
     * Update goal status based on progress and other factors
     * 
     * @param goal goal to update status for
     * @return updated status
     */
    Goal.Status updateGoalStatus(Goal goal);
    
    /**
     * Check if goal should be marked as completed
     * 
     * @param goal goal to check
     * @return true if goal should be completed
     */
    boolean shouldMarkAsCompleted(Goal goal);
    
    /**
     * Check if goal is overdue
     * 
     * @param goal goal to check
     * @return true if goal is overdue
     */
    boolean isOverdue(Goal goal);
    
    /**
     * Check if goal is at risk (behind schedule)
     * 
     * @param goal goal to check
     * @return true if goal is at risk
     */
    boolean isAtRisk(Goal goal);
    
    /**
     * Get status transition history for a goal
     * 
     * @param goal goal to get history for
     * @return list of status transitions
     */
    List<StatusTransition> getStatusHistory(Goal goal);
    
    /**
     * Get status statistics for user's goals
     * 
     * @param userId user ID
     * @return status distribution and metrics
     */
    Map<String, Object> getStatusStatistics(String userId);
    
    /**
     * Get goals by status
     * 
     * @param userId user ID
     * @param status status to filter by
     * @return list of goals with specified status
     */
    List<Goal> getGoalsByStatus(String userId, Goal.Status status);
    
    /**
     * Get goals that need status review
     * Goals that may need manual status intervention
     * 
     * @param userId user ID
     * @return list of goals needing review
     */
    List<Goal> getGoalsNeedingReview(String userId);
    
    /**
     * Validate status transition
     * Check if status change is allowed
     * 
     * @param currentStatus current status
     * @param newStatus proposed new status
     * @return true if transition is valid
     */
    boolean isValidStatusTransition(Goal.Status currentStatus, Goal.Status newStatus);
    
    /**
     * Get status recommendations
     * AI-driven suggestions for goal status
     * 
     * @param goal goal to analyze
     * @return status recommendations
     */
    Map<String, Object> getStatusRecommendations(Goal goal);
    
    /**
     * DTO for status transition tracking
     */
    class StatusTransition {
        private Goal.Status fromStatus;
        private Goal.Status toStatus;
        private LocalDateTime transitionDate;
        private String reason;
        private String userId;
        
        public StatusTransition(Goal.Status fromStatus, Goal.Status toStatus, LocalDateTime transitionDate, String reason, String userId) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.transitionDate = transitionDate;
            this.reason = reason;
            this.userId = userId;
        }
        
        // Getters and setters
        public Goal.Status getFromStatus() { return fromStatus; }
        public void setFromStatus(Goal.Status fromStatus) { this.fromStatus = fromStatus; }
        
        public Goal.Status getToStatus() { return toStatus; }
        public void setToStatus(Goal.Status toStatus) { this.toStatus = toStatus; }
        
        public LocalDateTime getTransitionDate() { return transitionDate; }
        public void setTransitionDate(LocalDateTime transitionDate) { this.transitionDate = transitionDate; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
