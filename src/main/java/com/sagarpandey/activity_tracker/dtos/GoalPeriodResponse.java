package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class GoalPeriodResponse {

    private Long id;
    private String uuid;
    private String goalId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Double currentValue;
    private Double progressPercentage;
    private HealthStatus healthStatus;
    private Double healthScore;
    private Double consistencyScore;
    private Double momentumScore;
    private Double progressScore;
    private Integer currentStreak;
    private Integer longestStreak;
    private Double minimumSessionDaily;
    private ScheduleSpec scheduleSpec;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }
    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public Double getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(Double consistencyScore) { this.consistencyScore = consistencyScore; }
    public Double getMomentumScore() { return momentumScore; }
    public void setMomentumScore(Double momentumScore) { this.momentumScore = momentumScore; }
    public Double getProgressScore() { return progressScore; }
    public void setProgressScore(Double progressScore) { this.progressScore = progressScore; }
    public Integer getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(Integer currentStreak) { this.currentStreak = currentStreak; }
    public Integer getLongestStreak() { return longestStreak; }
    public void setLongestStreak(Integer longestStreak) { this.longestStreak = longestStreak; }
    public Double getMinimumSessionDaily() { return minimumSessionDaily; }
    public void setMinimumSessionDaily(Double minimumSessionDaily) { this.minimumSessionDaily = minimumSessionDaily; }
    public ScheduleSpec getScheduleSpec() { return scheduleSpec; }
    public void setScheduleSpec(ScheduleSpec scheduleSpec) { this.scheduleSpec = scheduleSpec; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
