package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.enums.ScheduleType;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.LocalDateTime;
import java.util.List;

public class GoalResponse {
    
    private Long id;
    private String uuid;
    private String userId;
    private String title;
    private String description;
    private Goal.Priority priority;
    private Goal.Status status;
    private Goal.Metric metric;
    private Goal.TargetOperator targetOperator;
    private Double targetValue;
    private Double currentValue;
    private Double progressPercentage;
    private LocalDateTime startDate;
    private LocalDateTime targetDate;
    private LocalDateTime completedDate;
    private String parentGoalId;
    private Boolean isMilestone;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    
    private List<GoalResponse> childGoals;

    // === NEW FIELDS ===
    private GoalType goalType;
    private Boolean isLeaf;
    private Boolean isTracked;

    // Time-Bounded Ledger Bounds
    private ScheduleType scheduleType;
    private ScheduleSpec scheduleSpec;
    private Integer minimumSessionPeriod;
    private Integer maximumSessionPeriod;
    private Integer minimumTimeCommittedPeriod;
    private Integer minimumTimeCommittedDaily;
    private Boolean allowDoubleLogging;
    private Integer missesAllowedPerPeriod;
    private String scheduleDays;
    private List<com.sagarpandey.activity_tracker.enums.ScheduleDay> scheduleDaysList;

    // Weights
    private Integer effectiveConsistencyWeight;
    private Integer effectiveMomentumWeight;
    private Integer effectiveProgressWeight;
    private Integer consistencyWeight;
    private Integer momentumWeight;
    private Integer progressWeight;

    // Scores
    private Double consistencyScore;
    private Double momentumScore;
    private Double healthScore;
    private HealthStatus healthStatus;

    // Streaks
    private Integer currentStreak;
    private Integer longestStreak;

    // Rollup Insights
    private ParentInsights parentInsights;

    public GoalResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDateTime targetDate) { this.targetDate = targetDate; }

    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }

    public String getParentGoalId() { return parentGoalId; }
    public void setParentGoalId(String parentGoalId) { this.parentGoalId = parentGoalId; }

    public Boolean getIsMilestone() { return isMilestone; }
    public void setIsMilestone(Boolean isMilestone) { this.isMilestone = isMilestone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public List<GoalResponse> getChildGoals() { return childGoals; }
    public void setChildGoals(List<GoalResponse> childGoals) { this.childGoals = childGoals; }

    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }

    public Boolean getIsLeaf() { return isLeaf; }
    public void setIsLeaf(Boolean isLeaf) { this.isLeaf = isLeaf; }

    public Boolean getIsTracked() { return isTracked; }
    public void setIsTracked(Boolean isTracked) { this.isTracked = isTracked; }

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

    public Integer getEffectiveConsistencyWeight() { return effectiveConsistencyWeight; }
    public void setEffectiveConsistencyWeight(Integer effectiveConsistencyWeight) { this.effectiveConsistencyWeight = effectiveConsistencyWeight; }

    public Integer getEffectiveMomentumWeight() { return effectiveMomentumWeight; }
    public void setEffectiveMomentumWeight(Integer effectiveMomentumWeight) { this.effectiveMomentumWeight = effectiveMomentumWeight; }

    public Integer getEffectiveProgressWeight() { return effectiveProgressWeight; }
    public void setEffectiveProgressWeight(Integer effectiveProgressWeight) { this.effectiveProgressWeight = effectiveProgressWeight; }

    public Integer getConsistencyWeight() { return consistencyWeight; }
    public void setConsistencyWeight(Integer consistencyWeight) { this.consistencyWeight = consistencyWeight; }

    public Integer getMomentumWeight() { return momentumWeight; }
    public void setMomentumWeight(Integer momentumWeight) { this.momentumWeight = momentumWeight; }

    public Integer getProgressWeight() { return progressWeight; }
    public void setProgressWeight(Integer progressWeight) { this.progressWeight = progressWeight; }

    public Double getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(Double consistencyScore) { this.consistencyScore = consistencyScore; }

    public Double getMomentumScore() { return momentumScore; }
    public void setMomentumScore(Double momentumScore) { this.momentumScore = momentumScore; }

    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }

    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }

    public Integer getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(Integer currentStreak) { this.currentStreak = currentStreak; }

    public Integer getLongestStreak() { return longestStreak; }
    public void setLongestStreak(Integer longestStreak) { this.longestStreak = longestStreak; }

    public ParentInsights getParentInsights() { return parentInsights; }
    public void setParentInsights(ParentInsights parentInsights) { this.parentInsights = parentInsights; }

    public String getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; }

    public List<com.sagarpandey.activity_tracker.enums.ScheduleDay> getScheduleDaysList() { return scheduleDaysList; }
    public void setScheduleDaysList(List<com.sagarpandey.activity_tracker.enums.ScheduleDay> scheduleDaysList) { this.scheduleDaysList = scheduleDaysList; }
}
