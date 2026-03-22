package com.sagarpandey.activity_tracker.models;

/*
 * PHASE 1 - NEW COLUMNS ADDED TO goals TABLE:
 * goal_type                VARCHAR(50)
 * target_frequency_weekly  INTEGER
 * target_volume_daily      INTEGER
 * schedule_type            VARCHAR(50)
 * schedule_days            VARCHAR(100)  -- e.g. "MON,WED,FRI"
 * minimum_session_minutes  INTEGER
 * allow_double_logging     BOOLEAN
 * misses_allowed_per_week  INTEGER
 * misses_allowed_per_month INTEGER
 * consistency_weight       INTEGER
 * momentum_weight          INTEGER
 * progress_weight          INTEGER
 * consistency_score        DOUBLE PRECISION
 * momentum_score           DOUBLE PRECISION
 * health_score             DOUBLE PRECISION
 * health_status            VARCHAR(50)
 * current_streak           INTEGER
 * longest_streak           INTEGER
 */

import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.enums.ScheduleDay;
import com.sagarpandey.activity_tracker.enums.ScheduleType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "goals", uniqueConstraints = {@UniqueConstraint(columnNames = {"uuid"})})
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

    @Column(length = 1000)
    private String notes;

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
    @Column(name = "target_frequency_weekly")
    private Integer targetFrequencyWeekly;
    // How many times per week (e.g. 5)

    @Column(name = "target_volume_daily")
    private Integer targetVolumeDaily;
    // How many units of work per day (e.g. 1)

    // --- Schedule Configuration ---
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type")
    private ScheduleType scheduleType;
    // FLEXIBLE or SPECIFIC_DAYS
    // Default driven by goalType when goal is created

    @Column(name = "schedule_days")
    private String scheduleDays;
    // Stored as comma separated string: "MON,WED,FRI"
    // Only used when scheduleType = SPECIFIC_DAYS
    // Frontend sends array, backend serializes to string

    @Column(name = "minimum_session_minutes")
    private Integer minimumSessionMinutes;
    // Activity only counts toward goal if duration >= this value
    // Null means any duration counts

    @Column(name = "allow_double_logging")
    private Boolean allowDoubleLogging;
    // Can user log same goal twice in one day?
    // Default: true

    // --- Grace Period Configuration ---
    // Per-goal overrides. If null, falls back to user global preference.
    // User global preference falls back to priority-based defaults:
    // P1/CRITICAL → 0, P2/HIGH → 1, P3/MEDIUM → 2, P4/LOW → 3
    @Column(name = "misses_allowed_per_week")
    private Integer missesAllowedPerWeek;

    @Column(name = "misses_allowed_per_month")
    private Integer missesAllowedPerMonth;

    // --- Health Score Weights ---
    // Must sum to 100 when all three are set
    // If null, driven by GoalType defaults:
    // HABIT:   consistency=60, momentum=30, progress=10
    // PROJECT: consistency=20, momentum=20, progress=60
    // SKILL:   consistency=40, momentum=30, progress=30
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

    @Column(name = "health_score")
    private Double healthScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status")
    private HealthStatus healthStatus;

    // --- Streak Tracking ---
    @Column(name = "current_streak")
    private Integer currentStreak;
    // Consecutive weeks where targetFrequencyWeekly was met
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

    // === NEW FIELDS - PHASE 9 ===

    // The period over which progress/consistency is evaluated
    // If null, falls back to weekly system (existing behavior)
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_period")
    private EvaluationPeriod evaluationPeriod;

    // Target count per evaluation period
    // e.g. 4 books per MONTHLY period
    // e.g. 2 problems per DAILY period
    // If null, uses targetFrequencyWeekly for WEEKLY period
    @Column(name = "target_per_period")
    private Integer targetPerPeriod;

    // Only used when evaluationPeriod = CUSTOM
    // Defines the number of days in one custom period
    // e.g. customPeriodDays=10 means target resets every 10 days
    @Column(name = "custom_period_days")
    private Integer customPeriodDays;

    // Stores the start date of the current evaluation period
    // Updated automatically when period rolls over
    // e.g. for MONTHLY: first day of current month
    // e.g. for DAILY: today
    // e.g. for CUSTOM: date when current period started
    @Column(name = "current_period_start")
    private LocalDate currentPeriodStart;

    // Activities logged in the current evaluation period
    // Reset to 0 when period rolls over
    // Incremented when activity linked to this goal is logged
    @Column(name = "current_period_count")
    private Integer currentPeriodCount;

    // Consistency score for current evaluation period
    // = min(100, currentPeriodCount / targetPerPeriod * 100)
    // Recalculated on every activity log
    @Column(name = "period_consistency_score")
    private Double periodConsistencyScore;

    // Default constructor
    public Goal() {}

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

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Metric getMetric() { return metric; }
    public void setMetric(Metric metric) { this.metric = metric; }

    public TargetOperator getTargetOperator() { return targetOperator; }
    public void setTargetOperator(TargetOperator targetOperator) { this.targetOperator = targetOperator; }

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

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }

    public Integer getTargetFrequencyWeekly() { return targetFrequencyWeekly; }
    public void setTargetFrequencyWeekly(Integer targetFrequencyWeekly) { this.targetFrequencyWeekly = targetFrequencyWeekly; }

    public Integer getTargetVolumeDaily() { return targetVolumeDaily; }
    public void setTargetVolumeDaily(Integer targetVolumeDaily) { this.targetVolumeDaily = targetVolumeDaily; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public String getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; }

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

    // === PHASE 9 GETTERS AND SETTERS ===

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

    // Returns the start of the current evaluation period
    // based on evaluationPeriod type and a reference date
    public LocalDate computePeriodStart(LocalDate referenceDate) {
        if (evaluationPeriod == null) return null;
        return switch (evaluationPeriod) {
            case DAILY -> referenceDate;
            case WEEKLY -> referenceDate.with(
                java.time.temporal.TemporalAdjusters
                    .previousOrSame(java.time.DayOfWeek.MONDAY)
            );
            case MONTHLY -> referenceDate.withDayOfMonth(1);
            case QUARTERLY -> {
                int month = referenceDate.getMonthValue();
                int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
                yield referenceDate
                    .withMonth(quarterStartMonth)
                    .withDayOfMonth(1);
            }
            case YEARLY -> referenceDate.withDayOfYear(1);
            case CUSTOM -> {
                // currentPeriodStart is managed externally
                // return existing or today if null
                yield currentPeriodStart != null
                    ? currentPeriodStart
                    : referenceDate;
            }
        };
    }

    // Returns true if the given date is within the current
    // evaluation period
    public boolean isInCurrentPeriod(LocalDate date) {
        if (evaluationPeriod == null || currentPeriodStart == null)
            return false;
        LocalDate periodStart = computePeriodStart(date);
        return !date.isBefore(periodStart);
    }

    // Returns default scheduleType based on goalType
    public ScheduleType getDefaultScheduleType() {
        if (this.goalType == null) return ScheduleType.FLEXIBLE;
        return switch (this.goalType) {
            case HABIT, FITNESS -> ScheduleType.SPECIFIC_DAYS;
            default -> ScheduleType.FLEXIBLE;
        };
    }

    // Returns effective consistency weight (own or goalType default)
    public int getEffectiveConsistencyWeight() {
        if (this.consistencyWeight != null) return this.consistencyWeight;
        if (this.goalType == null) return 34;
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
        if (this.momentumWeight != null) return this.momentumWeight;
        if (this.goalType == null) return 33;
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
        if (this.progressWeight != null) return this.progressWeight;
        if (this.goalType == null) return 33;
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
    public int getDefaultMissesAllowedPerWeek() {
        if (this.missesAllowedPerWeek != null)
            return this.missesAllowedPerWeek;
        if (this.priority == null) return 1;
        return switch (this.priority) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    // Converts scheduleDays string to List<ScheduleDay>
    // Returns empty list if null or FLEXIBLE
    public List<ScheduleDay> getScheduleDaysList() {
        if (this.scheduleDays == null || this.scheduleDays.isBlank())
            return List.of();
        return Arrays.stream(this.scheduleDays.split(","))
            .map(String::trim)
            .map(ScheduleDay::valueOf)
            .collect(Collectors.toList());
    }

    // Converts List<ScheduleDay> to comma separated string for storage
    public void setScheduleDaysList(List<ScheduleDay> days) {
        if (days == null || days.isEmpty()) {
            this.scheduleDays = null;
            return;
        }
        this.scheduleDays = days.stream()
            .map(ScheduleDay::name)
            .collect(Collectors.joining(","));
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
