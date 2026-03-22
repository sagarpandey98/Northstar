package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.enums.ScheduleDay;
import com.sagarpandey.activity_tracker.enums.ScheduleType;
import com.sagarpandey.activity_tracker.models.Goal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class GoalRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    private String notes;
    
    @NotNull(message = "Priority is required")
    private Goal.Priority priority;
    
    private Goal.Status status;
    
    @NotNull(message = "Metric is required")
    private Goal.Metric metric;
    
    @NotNull(message = "Target operator is required")
    private Goal.TargetOperator targetOperator;
    
    @NotNull(message = "Target value is required")
    @Min(value = 0, message = "Target value must be non-negative")
    private Double targetValue;
    
    @Min(value = 0, message = "Current value must be non-negative")
    private Double currentValue = 0.0;
    
    private LocalDateTime startDate;
    private LocalDateTime targetDate;
    private String parentGoalId;
    private Boolean isMilestone = false;

    // === NEW FIELDS - PHASE 2 ===

    // Goal type drives default weights and schedule behavior
    // Optional — if not provided, defaults to GENERAL
    private GoalType goalType;

    // Frequency targets — only relevant for leaf goals
    // Optional
    private Integer targetFrequencyWeekly;
    private Integer targetVolumeDaily;

    // Schedule configuration
    // Optional — if not provided, derived from goalType:
    //   HABIT/FITNESS → SPECIFIC_DAYS
    //   PROJECT/SKILL/GENERAL → FLEXIBLE
    private ScheduleType scheduleType;

    // Only used when scheduleType = SPECIFIC_DAYS
    // Optional — list of days e.g. ["MON", "WED", "FRI"]
    private List<ScheduleDay> scheduleDays;

    // Optional — activity only counts if duration >= this (minutes)
    private Integer minimumSessionMinutes;

    // Optional — default true
    private Boolean allowDoubleLogging;

    // Grace period — optional, overrides user global preference
    // If null, falls back to priority-based default:
    // CRITICAL→0, HIGH→1, MEDIUM→2, LOW→3
    private Integer missesAllowedPerWeek;
    private Integer missesAllowedPerMonth;

    // Health score weights — optional
    // Rule: if ANY of the three weights is provided, 
    // ALL THREE must be provided and must sum to 100
    // Validation handled in STEP 3 below
    private Integer consistencyWeight;
    private Integer momentumWeight;
    private Integer progressWeight;

    // Default constructor
    public GoalRequest() {}

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Goal.Priority getPriority() { return priority; }
    public void setPriority(Goal.Priority priority) { this.priority = priority; }

    public Goal.Status getStatus() { return status; }
    public void setStatus(Goal.Status status) { this.status = status; }

    public Goal.Metric getMetric() { return metric; }
    public void setMetric(Goal.Metric metric) { this.metric = metric; }

    public Goal.TargetOperator getTargetOperator() { return targetOperator; }
    public void setTargetOperator(Goal.TargetOperator targetOperator) { this.targetOperator = targetOperator; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDateTime targetDate) { this.targetDate = targetDate; }

    public String getParentGoalId() { return parentGoalId; }
    public void setParentGoalId(String parentGoalId) { this.parentGoalId = parentGoalId; }

    public Boolean getIsMilestone() { return isMilestone; }
    public void setIsMilestone(Boolean isMilestone) { this.isMilestone = isMilestone; }

    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }

    public Integer getTargetFrequencyWeekly() { return targetFrequencyWeekly; }
    public void setTargetFrequencyWeekly(Integer targetFrequencyWeekly) { this.targetFrequencyWeekly = targetFrequencyWeekly; }

    public Integer getTargetVolumeDaily() { return targetVolumeDaily; }
    public void setTargetVolumeDaily(Integer targetVolumeDaily) { this.targetVolumeDaily = targetVolumeDaily; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public List<ScheduleDay> getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(List<ScheduleDay> scheduleDays) { this.scheduleDays = scheduleDays; }

    public Integer getMinimumSessionMinutes() { return minimumSessionMinutes; }
    public void setMinimumSessionMinutes(Integer minimumSessionMinutes) { this.minimumSessionMinutes = minimumSessionMinutes; }

    public Boolean getAllowDoubleLogging() { return allowDoubleLogging; }
    public void setAllowDoubleLogging(Boolean allowDoubleLogging) { this.allowDoubleLogging = allowDoubleLogging; }

    public Integer getMissesAllowedPerWeek() { return missesAllowedPerWeek; }
    public void setMissesAllowedPerWeek(Integer missesAllowedPerWeek) { this.missesAllowedPerWeek = missesAllowedPerWeek; }

    public Integer getMissesAllowedPerMonth() { return missesAllowedPerMonth; }
    public void setMissesAllowedPerMonth(Integer missesAllowedPerMonth) { this.missesAllowedPerMonth = missesAllowedPerMonth; }

    public Integer getConsistencyWeight() { return consistencyWeight; }
    public void setConsistencyWeight(Integer consistencyWeight) { this.consistencyWeight = consistencyWeight; }

    public Integer getMomentumWeight() { return momentumWeight; }
    public void setMomentumWeight(Integer momentumWeight) { this.momentumWeight = momentumWeight; }

    public Integer getProgressWeight() { return progressWeight; }
    public void setProgressWeight(Integer progressWeight) { this.progressWeight = progressWeight; }

    // === NEW FIELDS - PHASE 9 ===
    private EvaluationPeriod evaluationPeriod;
    private Integer targetPerPeriod;
    private Integer customPeriodDays;

    public EvaluationPeriod getEvaluationPeriod() 
        { return evaluationPeriod; }
    public void setEvaluationPeriod(EvaluationPeriod evaluationPeriod) 
        { this.evaluationPeriod = evaluationPeriod; }

    public Integer getTargetPerPeriod() { return targetPerPeriod; }
    public void setTargetPerPeriod(Integer targetPerPeriod) 
        { this.targetPerPeriod = targetPerPeriod; }

    public Integer getCustomPeriodDays() { return customPeriodDays; }
    public void setCustomPeriodDays(Integer customPeriodDays) 
        { this.customPeriodDays = customPeriodDays; }
}
