package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.models.Goal;
import java.util.Map;

public class GoalStatsResponse {
    
    private int totalGoals;
    private int completedGoals;
    private int inProgressGoals;
    private int notStartedGoals;
    private int overdueGoals;
    private int dueSoonGoals;
    private int milestones;
    
    private double overallCompletionPercentage;
    private double overduePercentage;
    
    // Breakdown by priority
    private Map<Goal.Priority, Integer> goalsByPriority;
    
    // Breakdown by status
    private Map<Goal.Status, Integer> goalsByStatus;

    // === NEW FIELDS - PHASE 7 ===
    
    // Count of goals by health status
    private Integer thrivingGoals;
    private Integer onTrackGoals;
    private Integer atRiskGoals;
    private Integer criticalGoals;
    private Integer untrackedGoals;

    // Average health score across all goals
    // Null if no goals have health scores yet
    private Double averageHealthScore;

    // Count of goals that have tracking configured
    // (targetFrequencyWeekly is set)
    private Integer trackedGoals;

    // Count of goals that are leaf nodes
    // (have no children — actual trackable goals)
    private Integer leafGoals;

    // Default constructor
    public GoalStatsResponse() {}

    // Getters and setters
    public int getTotalGoals() { return totalGoals; }
    public void setTotalGoals(int totalGoals) { this.totalGoals = totalGoals; }

    public int getCompletedGoals() { return completedGoals; }
    public void setCompletedGoals(int completedGoals) { this.completedGoals = completedGoals; }

    public int getInProgressGoals() { return inProgressGoals; }
    public void setInProgressGoals(int inProgressGoals) { this.inProgressGoals = inProgressGoals; }

    public int getNotStartedGoals() { return notStartedGoals; }
    public void setNotStartedGoals(int notStartedGoals) { this.notStartedGoals = notStartedGoals; }

    public int getOverdueGoals() { return overdueGoals; }
    public void setOverdueGoals(int overdueGoals) { this.overdueGoals = overdueGoals; }

    public int getDueSoonGoals() { return dueSoonGoals; }
    public void setDueSoonGoals(int dueSoonGoals) { this.dueSoonGoals = dueSoonGoals; }

    public int getMilestones() { return milestones; }
    public void setMilestones(int milestones) { this.milestones = milestones; }

    public double getOverallCompletionPercentage() { return overallCompletionPercentage; }
    public void setOverallCompletionPercentage(double overallCompletionPercentage) { 
        this.overallCompletionPercentage = overallCompletionPercentage; 
    }

    public double getOverduePercentage() { return overduePercentage; }
    public void setOverduePercentage(double overduePercentage) { this.overduePercentage = overduePercentage; }

    public Map<Goal.Priority, Integer> getGoalsByPriority() { return goalsByPriority; }
    public void setGoalsByPriority(Map<Goal.Priority, Integer> goalsByPriority) { 
        this.goalsByPriority = goalsByPriority; 
    }

    public Map<Goal.Status, Integer> getGoalsByStatus() { return goalsByStatus; }
    public void setGoalsByStatus(Map<Goal.Status, Integer> goalsByStatus) { 
        this.goalsByStatus = goalsByStatus; 
    }

    public Integer getThrivingGoals() { return thrivingGoals; }
    public void setThrivingGoals(Integer thrivingGoals) { this.thrivingGoals = thrivingGoals; }

    public Integer getOnTrackGoals() { return onTrackGoals; }
    public void setOnTrackGoals(Integer onTrackGoals) { this.onTrackGoals = onTrackGoals; }

    public Integer getAtRiskGoals() { return atRiskGoals; }
    public void setAtRiskGoals(Integer atRiskGoals) { this.atRiskGoals = atRiskGoals; }

    public Integer getCriticalGoals() { return criticalGoals; }
    public void setCriticalGoals(Integer criticalGoals) { this.criticalGoals = criticalGoals; }

    public Integer getUntrackedGoals() { return untrackedGoals; }
    public void setUntrackedGoals(Integer untrackedGoals) { this.untrackedGoals = untrackedGoals; }

    public Double getAverageHealthScore() { return averageHealthScore; }
    public void setAverageHealthScore(Double averageHealthScore) { this.averageHealthScore = averageHealthScore; }

    public Integer getTrackedGoals() { return trackedGoals; }
    public void setTrackedGoals(Integer trackedGoals) { this.trackedGoals = trackedGoals; }

    public Integer getLeafGoals() { return leafGoals; }
    public void setLeafGoals(Integer leafGoals) { this.leafGoals = leafGoals; }
}
