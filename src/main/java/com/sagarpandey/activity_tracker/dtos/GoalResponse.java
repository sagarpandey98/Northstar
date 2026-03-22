package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.enums.ScheduleDay;
import com.sagarpandey.activity_tracker.enums.ScheduleType;
import com.sagarpandey.activity_tracker.models.Goal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GoalResponse {
    
    private Long id;
    private String uuid;
    private String userId;
    private String title;
    private String description;
    private String notes;
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
    
    // Child goals for hierarchical structure
    private List<GoalResponse> childGoals;

    // === NEW FIELDS - PHASE 2 ===

    // Goal type
    private GoalType goalType;

    // Leaf/parent flag
    // true if this goal has no children
    // Computed at query time, never stored in DB
    private Boolean isLeaf;

    // Tracking configured flag
    // true if targetFrequencyWeekly is set
    // Computed at mapping time
    // Frontend uses this to show "Set up tracking" prompt
    private Boolean isTracked;

    // Frequency targets
    private Integer targetFrequencyWeekly;
    private Integer targetVolumeDaily;

    // Schedule configuration
    private ScheduleType scheduleType;
    private List<ScheduleDay> scheduleDays;
    private Integer minimumSessionMinutes;
    private Boolean allowDoubleLogging;

    // Grace period
    private Integer missesAllowedPerWeek;
    private Integer missesAllowedPerMonth;

    // Effective weights (always returned, even if not explicitly set)
    // These reflect the actual weights used for health calculation
    // Derived from goalType defaults if not explicitly configured
    private Integer effectiveConsistencyWeight;
    private Integer effectiveMomentumWeight;
    private Integer effectiveProgressWeight;

    // Raw weights (only set if user explicitly configured them)
    // Null if using goalType defaults
    private Integer consistencyWeight;
    private Integer momentumWeight;
    private Integer progressWeight;

    // Health scores
    private Double consistencyScore;
    private Double momentumScore;
    private Double healthScore;
    private HealthStatus healthStatus;

    // Streak data
    private Integer currentStreak;
    private Integer longestStreak;

    // Parent insights block
    // Only populated for non-leaf goals (isLeaf = false)
    // Null for leaf goals
    private ParentInsights parentInsights;

    // Default constructor
    public GoalResponse() {}

    // Getters and setters
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

    // === NEW FIELDS - PHASE 9 ===
    private EvaluationPeriod evaluationPeriod;
    private Integer targetPerPeriod;
    private Integer customPeriodDays;
    private LocalDate currentPeriodStart;
    private Integer currentPeriodCount;
    private Double periodConsistencyScore;

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

    public LocalDate getCurrentPeriodStart() 
        { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDate currentPeriodStart) 
        { this.currentPeriodStart = currentPeriodStart; }

    public Integer getCurrentPeriodCount() 
        { return currentPeriodCount; }
    public void setCurrentPeriodCount(Integer currentPeriodCount) 
        { this.currentPeriodCount = currentPeriodCount; }

    public Double getPeriodConsistencyScore() 
        { return periodConsistencyScore; }
    public void setPeriodConsistencyScore(Double periodConsistencyScore) 
        { this.periodConsistencyScore = periodConsistencyScore; }
}
