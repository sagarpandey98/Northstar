package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Service.Interface.GoalStatusService;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of GoalStatusService providing centralized status management logic
 */
@Service
@Transactional
public class GoalStatusServiceV1 implements GoalStatusService {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Override
    public Goal.Status determineGoalStatus(Goal goal) {
        // Check if already completed
        if (goal.getStatus() == Goal.Status.COMPLETED) {
            return Goal.Status.COMPLETED;
        }
        
        // Check if should be marked as completed
        if (shouldMarkAsCompleted(goal)) {
            return Goal.Status.COMPLETED;
        }
        
        // Check if overdue
        if (isOverdue(goal)) {
            return Goal.Status.OVERDUE;
        }
        
        // Check if at risk - use IN_PROGRESS with risk flag for now
        if (isAtRisk(goal)) {
            return Goal.Status.IN_PROGRESS;
        }
        
        // Check if on track - use IN_PROGRESS for active goals
        if (isOnTrack(goal)) {
            return Goal.Status.IN_PROGRESS;
        }
        
        // Default to NOT_STARTED or IN_PROGRESS based on activity
        return hasActivity(goal) ? Goal.Status.IN_PROGRESS : Goal.Status.NOT_STARTED;
    }
    
    @Override
    public Goal.Status updateGoalStatus(Goal goal) {
        Goal.Status newStatus = determineGoalStatus(goal);
        Goal.Status oldStatus = goal.getStatus();
        
        if (!newStatus.equals(oldStatus)) {
            // Log status transition (could be extended to save to database)
            logStatusTransition(goal, oldStatus, newStatus, "Automatic status update");
            
            // Update goal status
            goal.setStatus(newStatus);
            goal.setLastUpdatedAt(LocalDateTime.now());
            
            // Set completed date if newly completed
            if (newStatus == Goal.Status.COMPLETED && goal.getCompletedDate() == null) {
                goal.setCompletedDate(LocalDateTime.now());
            }
            
            // Save goal
            goalRepository.save(goal);
        }
        
        return newStatus;
    }
    
    @Override
    public boolean shouldMarkAsCompleted(Goal goal) {
        // Check if target value is reached
        if (goal.getTargetValue() != null && goal.getCurrentValue() != null) {
            if (goal.getCurrentValue() >= goal.getTargetValue()) {
                return true;
            }
        }
        
        // Check if progress percentage indicates completion
        if (goal.getProgressPercentage() != null && goal.getProgressPercentage() >= 100.0) {
            return true;
        }
        
        // Check if health score indicates completion
        if (goal.getHealthScore() != null && goal.getHealthScore() >= 95.0) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isOverdue(Goal goal) {
        if (goal.getTargetDate() == null) {
            return false; // No target date, cannot be overdue
        }
        
        return LocalDate.now().isAfter(goal.getTargetDate().toLocalDate()) 
            && goal.getStatus() != Goal.Status.COMPLETED;
    }
    
    @Override
    public boolean isAtRisk(Goal goal) {
        // Check if health score is low
        if (goal.getHealthScore() != null && goal.getHealthScore() < 40.0) {
            return true;
        }
        
        // Check if consistency is poor
        if (goal.getConsistencyScore() != null && goal.getConsistencyScore() < 30.0) {
            return true;
        }
        
        // Check if approaching target date with low progress
        if (goal.getTargetDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate targetDate = goal.getTargetDate().toLocalDate();
            LocalDate halfwayPoint = goal.getStartDate() != null ? 
                goal.getStartDate().toLocalDate().plusDays(
                    java.time.temporal.ChronoUnit.DAYS.between(
                        goal.getStartDate().toLocalDate(), targetDate
                    ) / 2
                ) : targetDate.minusDays(30);
            
            if (today.isAfter(halfwayPoint) && 
                (goal.getProgressPercentage() == null || goal.getProgressPercentage() < 50.0)) {
                return true;
            }
        }
        
        // Check if streak is broken (for habit goals)
        if (goal.getCurrentStreak() != null && goal.getCurrentStreak() == 0 && 
            goal.getEvaluationPeriod() != null && 
            (goal.getEvaluationPeriod() == EvaluationPeriod.DAILY || 
             goal.getEvaluationPeriod() == EvaluationPeriod.WEEKLY)) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<StatusTransition> getStatusHistory(Goal goal) {
        // This would typically be stored in a separate status_history table
        // For now, return a simplified history based on current state
        List<StatusTransition> history = new ArrayList<>();
        
        // Add current status
        history.add(new StatusTransition(
            goal.getStatus(),
            goal.getStatus(),
            goal.getLastUpdatedAt(),
            "Current status",
            goal.getUserId()
        ));
        
        return history;
    }
    
    @Override
    public Map<String, Object> getStatusStatistics(String userId) {
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGoals", allGoals.size());
        
        // Status distribution
        Map<Goal.Status, Long> statusCounts = allGoals.stream()
            .collect(Collectors.groupingBy(Goal::getStatus, Collectors.counting()));
        
        stats.put("statusDistribution", statusCounts);
        
        // Calculate percentages
        Map<String, Double> statusPercentages = new HashMap<>();
        if (!allGoals.isEmpty()) {
            for (Map.Entry<Goal.Status, Long> entry : statusCounts.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / allGoals.size();
                statusPercentages.put(entry.getKey().toString(), percentage);
            }
        }
        stats.put("statusPercentages", statusPercentages);
        
        // Goals needing attention
        long overdueCount = allGoals.stream().filter(this::isOverdue).count();
        long atRiskCount = allGoals.stream().filter(this::isAtRisk).count();
        long onTrackCount = allGoals.stream().filter(this::isOnTrack).count();
        
        stats.put("overdueGoals", overdueCount);
        stats.put("atRiskGoals", atRiskCount);
        stats.put("onTrackGoals", onTrackCount);
        
        // Recently completed goals (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentlyCompleted = allGoals.stream()
            .filter(g -> g.getCompletedDate() != null && 
                       g.getCompletedDate().isAfter(thirtyDaysAgo))
            .count();
        
        stats.put("recentlyCompleted", recentlyCompleted);
        
        return stats;
    }
    
    @Override
    public List<Goal> getGoalsByStatus(String userId, Goal.Status status) {
        return goalRepository.findByUserIdAndIsDeletedFalse(userId).stream()
            .filter(goal -> goal.getStatus() == status)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Goal> getGoalsNeedingReview(String userId) {
        return goalRepository.findByUserIdAndIsDeletedFalse(userId).stream()
            .filter(goal -> {
                // Goals that are overdue or at risk need review
                if (isOverdue(goal) || isAtRisk(goal)) {
                    return true;
                }
                
                // Goals with no recent activity might need review
                if (goal.getLastUpdatedAt() != null) {
                    LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
                    if (goal.getLastUpdatedAt().isBefore(cutoff) && 
                        goal.getStatus() != Goal.Status.COMPLETED) {
                        return true;
                    }
                }
                
                return false;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isValidStatusTransition(Goal.Status currentStatus, Goal.Status newStatus) {
        // Define allowed status transitions using only existing enum values
        Set<Goal.Status> allowedFromNotStarted = Set.of(
            Goal.Status.NOT_STARTED, Goal.Status.IN_PROGRESS
        );
        
        Set<Goal.Status> allowedFromInProgress = Set.of(
            Goal.Status.IN_PROGRESS, Goal.Status.COMPLETED, Goal.Status.OVERDUE
        );
        
        Set<Goal.Status> allowedFromOverdue = Set.of(
            Goal.Status.OVERDUE, Goal.Status.IN_PROGRESS, Goal.Status.COMPLETED
        );
        
        // COMPLETED is typically final
        Set<Goal.Status> allowedFromCompleted = Set.of(Goal.Status.COMPLETED);
        
        return switch (currentStatus) {
            case NOT_STARTED -> allowedFromNotStarted.contains(newStatus);
            case IN_PROGRESS -> allowedFromInProgress.contains(newStatus);
            case OVERDUE -> allowedFromOverdue.contains(newStatus);
            case COMPLETED -> allowedFromCompleted.contains(newStatus);
        };
    }
    
    @Override
    public Map<String, Object> getStatusRecommendations(Goal goal) {
        Map<String, Object> recommendations = new HashMap<>();
        
        Goal.Status currentStatus = determineGoalStatus(goal);
        recommendations.put("currentStatus", currentStatus);
        recommendations.put("recommendedStatus", currentStatus);
        
        List<String> suggestions = new ArrayList<>();
        
        if (isOverdue(goal)) {
            suggestions.add("Consider extending target date or adjusting expectations");
            suggestions.add("Review if goal is still relevant and achievable");
        }
        
        if (isAtRisk(goal)) {
            suggestions.add("Increase activity frequency to improve consistency");
            suggestions.add("Review goal targets and adjust if needed");
            suggestions.add("Focus on building momentum with small wins");
        }
        
        if (shouldMarkAsCompleted(goal)) {
            suggestions.add("Celebrate completion and set new goals");
            suggestions.add("Document lessons learned for future goals");
        }
        
        if (currentStatus == Goal.Status.NOT_STARTED && hasActivity(goal)) {
            suggestions.add("Update status to IN_PROGRESS to reflect activity");
        }
        
        if (goal.getHealthScore() != null && goal.getHealthScore() > 80.0) {
            suggestions.add("Great progress! Consider increasing targets for next period");
        }
        
        recommendations.put("suggestions", suggestions);
        
        // Priority-based recommendations
        if (goal.getPriority() == Goal.Priority.CRITICAL && 
            (currentStatus == Goal.Status.OVERDUE || isAtRisk(goal))) {
            recommendations.put("priorityAlert", "Critical goal needs immediate attention");
        }
        
        return recommendations;
    }
    
    // Helper methods
    
    private boolean isOnTrack(Goal goal) {
        // Check if health score is good
        if (goal.getHealthScore() != null && goal.getHealthScore() >= 70.0) {
            return true;
        }
        
        // Check if consistency is good
        if (goal.getConsistencyScore() != null && goal.getConsistencyScore() >= 60.0) {
            return true;
        }
        
        // Check if progress is on track
        if (goal.getProgressPercentage() != null) {
            LocalDate today = LocalDate.now();
            if (goal.getTargetDate() != null && goal.getStartDate() != null) {
                LocalDate start = goal.getStartDate().toLocalDate();
                LocalDate target = goal.getTargetDate().toLocalDate();
                
                if (today.isBefore(target)) {
                    double expectedProgress = getExpectedProgress(start, target, today);
                    return goal.getProgressPercentage() >= expectedProgress;
                }
            }
        }
        
        return false;
    }
    
    private boolean hasActivity(Goal goal) {
        // This would typically check activity logs
        // For now, use health score as a proxy for activity
        return goal.getHealthScore() != null && goal.getHealthScore() > 0;
    }
    
    private double getExpectedProgress(LocalDate startDate, LocalDate targetDate, LocalDate currentDate) {
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, targetDate);
        long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, currentDate);
        
        if (totalDays <= 0) return 100.0;
        if (elapsedDays <= 0) return 0.0;
        
        return Math.min(100.0, (elapsedDays * 100.0) / totalDays);
    }
    
    private void logStatusTransition(Goal goal, Goal.Status fromStatus, Goal.Status toStatus, String reason) {
        // This would typically save to a status_history table
        // For now, just log the transition
        System.out.println(String.format(
            "Status transition for goal %s: %s -> %s (%s)",
            goal.getUuid(), fromStatus, toStatus, reason
        ));
    }
}
