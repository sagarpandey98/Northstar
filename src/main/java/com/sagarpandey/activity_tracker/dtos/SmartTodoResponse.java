package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.enums.GoalType;
import java.time.LocalDateTime;
import java.util.List;

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
    private String scheduleType; // V2 schedule type or CONFIGURED/FLEXIBLE display value
    private String scheduleDetails; // "MON/WED/FRI", "5 problems/day", etc.
    
    // Progress tracking
    private Integer currentProgress;  // X in X/Y
    private Integer targetProgress;    // Y in X/Y
    private Double progressPercentage;
    private boolean isCompletedToday;
    private String progressUnit;
    private String progressDisplay;

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

    // V1.1 Smart Todo signals
    private String todoStatus; // MUST_DO_TODAY, CATCH_UP_TODAY, GOOD_TO_DO_TODAY, COMPLETED_TODAY
    private Integer displayRank;
    private boolean recommendedFocus;
    private String primaryReasonCode;
    private List<String> reasonCodes;
    private List<String> reasonMessages;
    private List<ScoreComponent> scoreBreakdown;
    private String recommendedAction;
    private String scheduleLabel;
    private Integer remainingTodayTarget;
    private Integer remainingPeriodTarget;
    private Integer expectedProgressByToday;
    private Double paceRatio;
    private Integer periodCurrentProgress;
    private Integer periodTargetProgress;
    private Double periodProgressPercentage;
    private Integer totalActionableDays;
    private Integer elapsedActionableDays;
    private Integer remainingActionableDays;
    private String periodStartDate;
    private String periodEndDate;
    private boolean periodStartsToday;
    private Integer daysUntilTargetDate;
    private Double healthScoreSnapshot;

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

    public String getProgressUnit() { return progressUnit; }
    public void setProgressUnit(String progressUnit) { this.progressUnit = progressUnit; }

    public String getProgressDisplay() { return progressDisplay; }
    public void setProgressDisplay(String progressDisplay) { this.progressDisplay = progressDisplay; }

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

    public String getTodoStatus() { return todoStatus; }
    public void setTodoStatus(String todoStatus) { this.todoStatus = todoStatus; }

    public Integer getDisplayRank() { return displayRank; }
    public void setDisplayRank(Integer displayRank) { this.displayRank = displayRank; }

    public boolean isRecommendedFocus() { return recommendedFocus; }
    public void setRecommendedFocus(boolean recommendedFocus) { this.recommendedFocus = recommendedFocus; }

    public String getPrimaryReasonCode() { return primaryReasonCode; }
    public void setPrimaryReasonCode(String primaryReasonCode) { this.primaryReasonCode = primaryReasonCode; }

    public List<String> getReasonCodes() { return reasonCodes; }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes; }

    public List<String> getReasonMessages() { return reasonMessages; }
    public void setReasonMessages(List<String> reasonMessages) { this.reasonMessages = reasonMessages; }

    public List<ScoreComponent> getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(List<ScoreComponent> scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public String getScheduleLabel() { return scheduleLabel; }
    public void setScheduleLabel(String scheduleLabel) { this.scheduleLabel = scheduleLabel; }

    public Integer getRemainingTodayTarget() { return remainingTodayTarget; }
    public void setRemainingTodayTarget(Integer remainingTodayTarget) { this.remainingTodayTarget = remainingTodayTarget; }

    public Integer getRemainingPeriodTarget() { return remainingPeriodTarget; }
    public void setRemainingPeriodTarget(Integer remainingPeriodTarget) { this.remainingPeriodTarget = remainingPeriodTarget; }

    public Integer getExpectedProgressByToday() { return expectedProgressByToday; }
    public void setExpectedProgressByToday(Integer expectedProgressByToday) { this.expectedProgressByToday = expectedProgressByToday; }

    public Double getPaceRatio() { return paceRatio; }
    public void setPaceRatio(Double paceRatio) { this.paceRatio = paceRatio; }

    public Integer getPeriodCurrentProgress() { return periodCurrentProgress; }
    public void setPeriodCurrentProgress(Integer periodCurrentProgress) { this.periodCurrentProgress = periodCurrentProgress; }

    public Integer getPeriodTargetProgress() { return periodTargetProgress; }
    public void setPeriodTargetProgress(Integer periodTargetProgress) { this.periodTargetProgress = periodTargetProgress; }

    public Double getPeriodProgressPercentage() { return periodProgressPercentage; }
    public void setPeriodProgressPercentage(Double periodProgressPercentage) { this.periodProgressPercentage = periodProgressPercentage; }

    public Integer getTotalActionableDays() { return totalActionableDays; }
    public void setTotalActionableDays(Integer totalActionableDays) { this.totalActionableDays = totalActionableDays; }

    public Integer getElapsedActionableDays() { return elapsedActionableDays; }
    public void setElapsedActionableDays(Integer elapsedActionableDays) { this.elapsedActionableDays = elapsedActionableDays; }

    public Integer getRemainingActionableDays() { return remainingActionableDays; }
    public void setRemainingActionableDays(Integer remainingActionableDays) { this.remainingActionableDays = remainingActionableDays; }

    public String getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(String periodStartDate) { this.periodStartDate = periodStartDate; }

    public String getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(String periodEndDate) { this.periodEndDate = periodEndDate; }

    public boolean isPeriodStartsToday() { return periodStartsToday; }
    public void setPeriodStartsToday(boolean periodStartsToday) { this.periodStartsToday = periodStartsToday; }

    public Integer getDaysUntilTargetDate() { return daysUntilTargetDate; }
    public void setDaysUntilTargetDate(Integer daysUntilTargetDate) { this.daysUntilTargetDate = daysUntilTargetDate; }

    public Double getHealthScoreSnapshot() { return healthScoreSnapshot; }
    public void setHealthScoreSnapshot(Double healthScoreSnapshot) { this.healthScoreSnapshot = healthScoreSnapshot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public static class ScoreComponent {
        private String code;
        private String label;
        private Double value;

        public ScoreComponent() {}

        public ScoreComponent(String code, String label, Double value) {
            this.code = code;
            this.label = label;
            this.value = value;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
    }
}
