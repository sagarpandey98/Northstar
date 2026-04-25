package com.sagarpandey.activity_tracker.models;

/*
 * PHASE 1 - NEW COLUMNS ADDED TO goals TABLE:
 * goal_type                    VARCHAR(50)
 * target_frequency_weekly      INTEGER
 * target_volume_daily          INTEGER
 * schedule_type                VARCHAR(50)
 * schedule_days                VARCHAR(100)  -- e.g. "MON,WED,FRI"
 * minimum_session_period       INTEGER  -- mins for selected period
 * minimum_session_daily        INTEGER  -- auto-calculated daily mins
 * allow_double_logging         BOOLEAN
 * misses_allowed_per_week      INTEGER
 * misses_allowed_per_month     INTEGER
 * consistency_weight           INTEGER
 * momentum_weight              INTEGER
 * progress_weight              INTEGER
 * consistency_score            DOUBLE PRECISION
 * momentum_score               DOUBLE PRECISION
 * health_score                 DOUBLE PRECISION
 * health_status                VARCHAR(50)
 * current_streak               INTEGER
 * longest_streak               INTEGER
 */

import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.enums.ScheduleDay;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;

@Entity
@Table(name = "goals", uniqueConstraints = { @UniqueConstraint(columnNames = { "uuid" }) })
public class Goal {
    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE
    }

    public enum Metric {
        COUNT, DURATION, CUSTOM
    }

    public enum TargetOperator {
        GREATER_THAN, EQUAL, LESS_THAN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "schedule_spec", columnDefinition = "TEXT")
    @Convert(converter = com.sagarpandey.activity_tracker.Mapper.ScheduleSpecConverter.class)
    private ScheduleSpec scheduleSpec;
    @Column(name = "maximum_session_period")
    private Integer maximumSessionPeriod;

    @Column(name = "minimum_time_committed_period")
    private Integer minimumTimeCommittedPeriod;
    // How much time (in minutes) the user commits per period for this goal
    // e.g. 60 means "I can give at least 1 hour per period"

    @Column(name = "minimum_time_committed_daily")
    private Integer minimumTimeCommittedDaily;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Metric metric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetOperator targetOperator;

    @Column(nullable = false)
    private Double targetValue;

    @Column(nullable = false)
    private Double currentValue = 0.0;

    @Column(nullable = false)
    private Double progressPercentage = 0.0;

    private LocalDateTime startDate;
    private LocalDateTime targetDate;
    private LocalDateTime completedDate;

    private String parentGoalId;
    private Boolean isMilestone = false;

    // Audit fields
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();
    private Boolean isDeleted = false;

    // === NEW FIELDS - PHASE 1 ===

    // --- Goal Type ---
    // Drives default weights and schedule behavior
    // HABIT/FITNESS → SPECIFIC_DAYS default
    // PROJECT/SKILL/GENERAL → FLEXIBLE default
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type")
    private GoalType goalType;

    // --- Frequency Targets ---
    // Only meaningful on leaf goals (goals with no children)

    // --- Schedule Configuration ---
    // Schedule configuration now handled by scheduleSpec field

    @Column(name = "minimum_session_period")
    private Integer minimumSessionPeriod;
    // Minimum time (in minutes) required per evaluation period
    // e.g. 120 min per week, 300 min per month
    // Used to determine if goal is on track for priority calculation
    // Null means no minimum time requirement

    @Column(name = "minimum_session_daily")
    private Double minimumSessionDaily;
    // Auto-calculated daily minimum
    // = minimumSessionPeriod / number_of_days_in_period
    // e.g. 120 min week / 7 days = ~17 min daily
    // Used for daily urgency and priority in smart todo list

    @Column(name = "allow_double_logging")
    private Boolean allowDoubleLogging;
    // Can user log same goal twice in one day?
    // Default: true

    // --- Grace Period Configuration ---
    // Per-goal overrides. If null, falls back to user global preference.
    // User global preference falls back to priority-based defaults:
    // P1/CRITICAL → 0, P2/HIGH → 1, P3/MEDIUM → 2, P4/LOW → 3
    @Column(name = "misses_allowed_per_period")
    private Integer missesAllowedPerPeriod;

    // --- Health Score Weights ---
    // Must sum to 100 when all three are set
    // If null, driven by GoalType defaults:
    // HABIT: consistency=60, momentum=30, progress=10
    // PROJECT: consistency=20, momentum=20, progress=60
    // SKILL: consistency=40, momentum=30, progress=30
    // FITNESS: consistency=50, momentum=40, progress=10
    // GENERAL: consistency=34, momentum=33, progress=33
    @Column(name = "consistency_weight")
    private Integer consistencyWeight;

    @Column(name = "momentum_weight")
    private Integer momentumWeight;

    @Column(name = "progress_weight")
    private Integer progressWeight;

    // --- Calculated Health Scores ---
    // Recalculated every time an activity linked to this goal is logged
    // Also recalculated by weekly scheduled job
    @Column(name = "consistency_score")
    private Double consistencyScore;

    @Column(name = "momentum_score")
    private Double momentumScore;

    @Column(name = "progress_score")
    private Double progressScore;

    @Column(name = "health_score")
    private Double healthScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status")
    private HealthStatus healthStatus;

    // --- Streak Tracking ---
    @Column(name = "current_streak")
    private Integer currentStreak;
    // Consecutive periods where the configured schedule/target was met
    // Reset to 0 if a week is missed beyond grace period

    @Column(name = "longest_streak")
    private Integer longestStreak;
    // Historical best streak, never decreases
    // Useful for motivation display in frontend

    // --- Leaf/Parent Flags ---
    // isLeaf is derived at query time, NOT stored
    // Backend computes it by checking if any child goals exist
    // DO NOT add isLeaf as a DB column
    // It will be added to the response DTO in a later phase

    // Default constructor
    public Goal() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public TargetOperator getTargetOperator() {
        return targetOperator;
    }

    public void setTargetOperator(TargetOperator targetOperator) {
        this.targetOperator = targetOperator;
    }

    public Double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Double targetValue) {
        this.targetValue = targetValue;
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDateTime targetDate) {
        this.targetDate = targetDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public String getParentGoalId() {
        return parentGoalId;
    }

    public void setParentGoalId(String parentGoalId) {
        this.parentGoalId = parentGoalId;
    }

    public Boolean getIsMilestone() {
        return isMilestone;
    }

    public void setIsMilestone(Boolean isMilestone) {
        this.isMilestone = isMilestone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }


    public Integer getMinimumSessionPeriod() {
        return minimumSessionPeriod;
    }

    public void setMinimumSessionPeriod(Integer minimumSessionPeriod) {
        this.minimumSessionPeriod = minimumSessionPeriod;
    }

    public Double getMinimumSessionDaily() {
        return minimumSessionDaily;
    }

    public void setMinimumSessionDaily(Double minimumSessionDaily) {
        this.minimumSessionDaily = minimumSessionDaily;
    }

    public Boolean getAllowDoubleLogging() {
        return allowDoubleLogging;
    }

    public void setAllowDoubleLogging(Boolean allowDoubleLogging) {
        this.allowDoubleLogging = allowDoubleLogging;
    }

    public Integer getMissesAllowedPerPeriod() {
        return missesAllowedPerPeriod;
    }

    public void setMissesAllowedPerPeriod(Integer missesAllowedPerPeriod) {
        this.missesAllowedPerPeriod = missesAllowedPerPeriod;
    }

    public Integer getConsistencyWeight() {
        return consistencyWeight;
    }

    public void setConsistencyWeight(Integer consistencyWeight) {
        this.consistencyWeight = consistencyWeight;
    }

    public Integer getMomentumWeight() {
        return momentumWeight;
    }

    public void setMomentumWeight(Integer momentumWeight) {
        this.momentumWeight = momentumWeight;
    }

    public Integer getProgressWeight() {
        return progressWeight;
    }

    public void setProgressWeight(Integer progressWeight) {
        this.progressWeight = progressWeight;
    }

    public Double getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(Double consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public Double getMomentumScore() {
        return momentumScore;
    }

    public void setMomentumScore(Double momentumScore) {
        this.momentumScore = momentumScore;
    }

    public Double getProgressScore() {
        return progressScore;
    }

    public void setProgressScore(Double progressScore) {
        this.progressScore = progressScore;
    }

    public Double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Double healthScore) {
        this.healthScore = healthScore;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Integer getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(Integer longestStreak) {
        this.longestStreak = longestStreak;
    }

    public Integer getMinimumTimeCommittedPeriod() {
        return minimumTimeCommittedPeriod;
    }

    public void setMinimumTimeCommittedPeriod(Integer minimumTimeCommittedPeriod) {
        this.minimumTimeCommittedPeriod = minimumTimeCommittedPeriod;
    }

    public Integer getMinimumTimeCommittedDaily() {
        return minimumTimeCommittedDaily;
    }

    public void setMinimumTimeCommittedDaily(Integer minimumTimeCommittedDaily) {
        this.minimumTimeCommittedDaily = minimumTimeCommittedDaily;
    }


    // Returns effective consistency weight (own or goalType default)
    public int getEffectiveConsistencyWeight() {
        if (this.consistencyWeight != null)
            return this.consistencyWeight;
        if (this.goalType == null)
            return 34;
        return switch (this.goalType) {
            case HABIT -> 60;
            case PROJECT -> 20;
            case SKILL -> 40;
            case FITNESS -> 50;
            default -> 34;
        };
    }

    // Returns effective momentum weight
    public int getEffectiveMomentumWeight() {
        if (this.momentumWeight != null)
            return this.momentumWeight;
        if (this.goalType == null)
            return 33;
        return switch (this.goalType) {
            case HABIT -> 30;
            case PROJECT -> 20;
            case SKILL -> 30;
            case FITNESS -> 40;
            default -> 33;
        };
    }

    // Returns effective progress weight
    public int getEffectiveProgressWeight() {
        if (this.progressWeight != null)
            return this.progressWeight;
        if (this.goalType == null)
            return 33;
        return switch (this.goalType) {
            case HABIT -> 10;
            case PROJECT -> 60;
            case SKILL -> 30;
            case FITNESS -> 10;
            default -> 33;
        };
    }

    // Returns default misses allowed per week based on priority
    // Used when per-goal missesAllowedPerWeek is null
    // and no global user preference is set
    public int getDefaultMissesAllowedPerPeriod() {
        if (this.missesAllowedPerPeriod != null)
            return this.missesAllowedPerPeriod;
        if (this.priority == null)
            return 1;
        return switch (this.priority) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    public ScheduleSpec getScheduleSpec() {
        return scheduleSpec;
    }

    public void setScheduleSpec(ScheduleSpec scheduleSpec) {
        this.scheduleSpec = scheduleSpec;
    }

    public Integer getMaximumSessionPeriod() {
        return maximumSessionPeriod;
    }

    public void setMaximumSessionPeriod(Integer maximumSessionPeriod) {
        this.maximumSessionPeriod = maximumSessionPeriod;
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
