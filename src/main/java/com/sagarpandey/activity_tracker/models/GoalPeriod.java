package com.sagarpandey.activity_tracker.models;

import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.enums.ScheduleType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goal_periods", uniqueConstraints = {@UniqueConstraint(columnNames = {"uuid"})})
public class GoalPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "goal_id", nullable = false)
    private String parentGoalUuid;

    // --- Calculation Metrics ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Goal.Metric metric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Goal.TargetOperator targetOperator;

    // Target value comes from parent Goal

    @Column(name = "current_value", nullable = false)
    private Double currentValue = 0.0;

    @Column(name = "progress_percentage", nullable = false)
    private Double progressPercentage = 0.0;

    // --- Period Boundaries (required) ---
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // --- Period Boundaries (already defined above) ---

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    // --- Schedule (scheduleSpec contains all scheduling info) ---

    @Column(name = "schedule_spec", columnDefinition = "text")
    @Convert(converter = com.sagarpandey.activity_tracker.Mapper.ScheduleSpecConverter.class)
    private ScheduleSpec scheduleSpec;

    @Column(name = "minimum_session_daily")
    private Double minimumSessionDaily;

    @Column(name = "minimum_session_period")
    private Integer minimumSessionPeriod;

    @Column(name = "maximum_session_period")
    private Integer maximumSessionPeriod;

    // Time commitment comes from parent Goal

    // --- Advanced ---
    @Column(name = "allow_double_logging")
    private Boolean allowDoubleLogging;

    // --- Health & Scoring ---
    @Column(name = "misses_allowed_per_period")
    private Integer missesAllowedPerPeriod;

    @Column(name = "health_score")
    private Double healthScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status")
    private HealthStatus healthStatus;

    @Column(name = "consistency_score")
    private Double consistencyScore;

    @Column(name = "momentum_score")
    private Double momentumScore;

    @Column(name = "progress_score")
    private Double progressScore;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "longest_streak")
    private Integer longestStreak;

    @Column(name = "consistency_weight")
    private Integer consistencyWeight;

    @Column(name = "momentum_weight")
    private Integer momentumWeight;

    @Column(name = "progress_weight")
    private Integer progressWeight;

    public GoalPeriod() {}

    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getParentGoalUuid() { return parentGoalUuid; }
    public void setParentGoalUuid(String parentGoalUuid) { this.parentGoalUuid = parentGoalUuid; }

    public Goal.Metric getMetric() { return metric; }
    public void setMetric(Goal.Metric metric) { this.metric = metric; }

    public Goal.TargetOperator getTargetOperator() { return targetOperator; }
    public void setTargetOperator(Goal.TargetOperator targetOperator) { this.targetOperator = targetOperator; }

    // Target value comes from parent Goal

    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    // Start/Target dates come from parent Goal

    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    // Schedule type comes from parent Goal via scheduleSpec

    public ScheduleSpec getScheduleSpec() { return scheduleSpec; }
    public void setScheduleSpec(ScheduleSpec scheduleSpec) { this.scheduleSpec = scheduleSpec; }

    public Double getMinimumSessionDaily() { return minimumSessionDaily; }
    public void setMinimumSessionDaily(Double minimumSessionDaily) { this.minimumSessionDaily = minimumSessionDaily; }

    public Integer getMinimumSessionPeriod() { return minimumSessionPeriod; }
    public void setMinimumSessionPeriod(Integer minimumSessionPeriod) { this.minimumSessionPeriod = minimumSessionPeriod; }

    public Integer getMaximumSessionPeriod() { return maximumSessionPeriod; }
    public void setMaximumSessionPeriod(Integer maximumSessionPeriod) { this.maximumSessionPeriod = maximumSessionPeriod; }

    // Time commitment comes from parent Goal

    public Boolean getAllowDoubleLogging() { return allowDoubleLogging; }
    public void setAllowDoubleLogging(Boolean allowDoubleLogging) { this.allowDoubleLogging = allowDoubleLogging; }

    public Integer getMissesAllowedPerPeriod() { return missesAllowedPerPeriod; }
    public void setMissesAllowedPerPeriod(Integer missesAllowedPerPeriod) { this.missesAllowedPerPeriod = missesAllowedPerPeriod; }

    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }

    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }

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

    public Integer getConsistencyWeight() { return consistencyWeight; }
    public void setConsistencyWeight(Integer consistencyWeight) { this.consistencyWeight = consistencyWeight; }

    public Integer getMomentumWeight() { return momentumWeight; }
    public void setMomentumWeight(Integer momentumWeight) { this.momentumWeight = momentumWeight; }

    public Integer getProgressWeight() { return progressWeight; }
    public void setProgressWeight(Integer progressWeight) { this.progressWeight = progressWeight; }
}
