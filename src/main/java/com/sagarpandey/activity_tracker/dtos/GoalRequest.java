package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.enums.ScheduleType;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoalRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
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

    // === NEW TIME BOUND LEDGER FIELDS ===

    private GoalType goalType;
    private ScheduleType scheduleType;
    private ScheduleSpec scheduleSpec;
    
    private Integer minimumSessionPeriod;
    private Integer maximumSessionPeriod;
    private Integer minimumTimeCommittedPeriod;
    private Integer minimumTimeCommittedDaily;

    private Boolean allowDoubleLogging;
    private Integer missesAllowedPerPeriod;

    private Integer consistencyWeight;
    private Integer momentumWeight;
    private Integer progressWeight;

    public GoalRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public ScheduleSpec getScheduleSpec() { return scheduleSpec; }
    public void setScheduleSpec(ScheduleSpec scheduleSpec) { this.scheduleSpec = scheduleSpec; }

    public Integer getMinimumSessionPeriod() { return minimumSessionPeriod; }
    public void setMinimumSessionPeriod(Integer minimumSessionPeriod) { this.minimumSessionPeriod = minimumSessionPeriod; }

    public Integer getMaximumSessionPeriod() { return maximumSessionPeriod; }
    public void setMaximumSessionPeriod(Integer maximumSessionPeriod) { this.maximumSessionPeriod = maximumSessionPeriod; }

    public Integer getMinimumTimeCommittedPeriod() { return minimumTimeCommittedPeriod; }
    public void setMinimumTimeCommittedPeriod(Integer minimumTimeCommittedPeriod) { this.minimumTimeCommittedPeriod = minimumTimeCommittedPeriod; }

    public Integer getMinimumTimeCommittedDaily() { return minimumTimeCommittedDaily; }
    public void setMinimumTimeCommittedDaily(Integer minimumTimeCommittedDaily) { this.minimumTimeCommittedDaily = minimumTimeCommittedDaily; }

    public Boolean getAllowDoubleLogging() { return allowDoubleLogging; }
    public void setAllowDoubleLogging(Boolean allowDoubleLogging) { this.allowDoubleLogging = allowDoubleLogging; }

    public Integer getMissesAllowedPerPeriod() { return missesAllowedPerPeriod; }
    public void setMissesAllowedPerPeriod(Integer missesAllowedPerPeriod) { this.missesAllowedPerPeriod = missesAllowedPerPeriod; }

    public Integer getConsistencyWeight() { return consistencyWeight; }
    public void setConsistencyWeight(Integer consistencyWeight) { this.consistencyWeight = consistencyWeight; }

    public Integer getMomentumWeight() { return momentumWeight; }
    public void setMomentumWeight(Integer momentumWeight) { this.momentumWeight = momentumWeight; }

    public Integer getProgressWeight() { return progressWeight; }
    public void setProgressWeight(Integer progressWeight) { this.progressWeight = progressWeight; }
}
