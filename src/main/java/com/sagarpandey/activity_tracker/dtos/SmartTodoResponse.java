package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.enums.GoalType;
import java.time.LocalDateTime;

/**
 * Smart Todo Response DTO for intelligent daily task generation
 * Represents a single todo item with rich context and progress tracking
 */
public class SmartTodoResponse {
    
    private Long goalId;
    private String title;
    private String description;
    private Goal.Priority priority;
    private GoalType goalType;
    
    // Priority display
    private String priorityDisplay; // Legacy (P1, P2)
    private Double urgencyScore;    // 0-100 calculated score
    private String smartPriorityGroup; // "Urgent", "High Priority", "Maintaining", etc.
    
    // Schedule information
    private boolean scheduledForToday;
    private String scheduleType; // DAILY, SPECIFIC_DAYS, WEEKLY, MONTHLY
    private String scheduleDetails; // "MON/WED/FRI", "5 problems/day", etc.
    
    // Progress tracking
    private Integer currentProgress;  // X in X/Y
    private Integer targetProgress;    // Y in X/Y
    private Double progressPercentage;
    private boolean isCompletedToday;
    
    // Streak and urgency
    private Integer currentStreak;
    private boolean streakAtRisk;
    private boolean isBehindSchedule;
    private String urgencyReason; // "Streak breaks today", "Behind weekly pace", etc.
    
    // Time suggestions
    private Integer minimumSessionPeriod;  // Total minimum time for period
    private Integer minimumSessionDaily;   // Auto-calculated daily minimum
    private Integer suggestedTimeMinutes;
    private String lastCompletedDate;
    
    // Activity logging context
    private boolean requiresQuickLog;
    private String quickLogContext; // Pre-filled context for logging
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    
    // Constructors
    public SmartTodoResponse() {}
    
    // Getters and Setters
    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Goal.Priority getPriority() { return priority; }
    public void setPriority(Goal.Priority priority) { this.priority = priority; }
    
    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }
    
    public String getPriorityDisplay() { return priorityDisplay; }
    public void setPriorityDisplay(String priorityDisplay) { this.priorityDisplay = priorityDisplay; }

    public Double getUrgencyScore() { return urgencyScore; }
    public void setUrgencyScore(Double urgencyScore) { this.urgencyScore = urgencyScore; }

    public String getSmartPriorityGroup() { return smartPriorityGroup; }
    public void setSmartPriorityGroup(String smartPriorityGroup) { this.smartPriorityGroup = smartPriorityGroup; }
    
    public boolean isScheduledForToday() { return scheduledForToday; }
    public void setScheduledForToday(boolean scheduledForToday) { this.scheduledForToday = scheduledForToday; }
    
    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }
    
    public String getScheduleDetails() { return scheduleDetails; }
    public void setScheduleDetails(String scheduleDetails) { this.scheduleDetails = scheduleDetails; }
    
    public Integer getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(Integer currentProgress) { this.currentProgress = currentProgress; }
    
    public Integer getTargetProgress() { return targetProgress; }
    public void setTargetProgress(Integer targetProgress) { this.targetProgress = targetProgress; }
    
    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }
    
    public boolean isCompletedToday() { return isCompletedToday; }
    public void setCompletedToday(boolean completedToday) { this.isCompletedToday = completedToday; }
    
    public Integer getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(Integer currentStreak) { this.currentStreak = currentStreak; }
    
    public boolean isStreakAtRisk() { return streakAtRisk; }
    public void setStreakAtRisk(boolean streakAtRisk) { this.streakAtRisk = streakAtRisk; }
    
    public boolean isBehindSchedule() { return isBehindSchedule; }
    public void setBehindSchedule(boolean behindSchedule) { this.isBehindSchedule = behindSchedule; }
    
    public String getUrgencyReason() { return urgencyReason; }
    public void setUrgencyReason(String urgencyReason) { this.urgencyReason = urgencyReason; }
    
    public Integer getMinimumSessionPeriod() { return minimumSessionPeriod; }
    public void setMinimumSessionPeriod(Integer minimumSessionPeriod) { this.minimumSessionPeriod = minimumSessionPeriod; }

    public Integer getMinimumSessionDaily() { return minimumSessionDaily; }
    public void setMinimumSessionDaily(Integer minimumSessionDaily) { this.minimumSessionDaily = minimumSessionDaily; }

    public Integer getSuggestedTimeMinutes() { return suggestedTimeMinutes; }
    public void setSuggestedTimeMinutes(Integer suggestedTimeMinutes) { this.suggestedTimeMinutes = suggestedTimeMinutes; }
    
    public String getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(String lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }
    
    public boolean isRequiresQuickLog() { return requiresQuickLog; }
    public void setRequiresQuickLog(boolean requiresQuickLog) { this.requiresQuickLog = requiresQuickLog; }
    
    public String getQuickLogContext() { return quickLogContext; }
    public void setQuickLogContext(String quickLogContext) { this.quickLogContext = quickLogContext; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
