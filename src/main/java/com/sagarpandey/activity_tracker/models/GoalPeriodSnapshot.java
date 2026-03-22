package com.sagarpandey.activity_tracker.models;

import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "goal_period_snapshots",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_goal_period",
            columnNames = {"goal_id", "period_type", "period_start"}
        )
    },
    indexes = {
        @Index(
            name = "idx_period_snapshot_goal",
            columnList = "goal_id, period_type, period_start"
        ),
        @Index(
            name = "idx_period_snapshot_user",
            columnList = "user_id, period_type, period_start"
        )
    }
)
public class GoalPeriodSnapshot {

    /*
     * PHASE 9 - TABLE: goal_period_snapshots
     *
     * goal_id              BIGINT        NOT NULL
     * user_id              VARCHAR(255)  NOT NULL
     * period_type          VARCHAR(50)   NOT NULL
     * period_start         DATE          NOT NULL
     * period_end           DATE          NOT NULL
     * activities_logged    INTEGER       NOT NULL DEFAULT 0
     * target_per_period    INTEGER       NULLABLE
     * consistency_score    DOUBLE        NULLABLE
     * created_at           TIMESTAMP
     * updated_at           TIMESTAMP
     *
     * UNIQUE: (goal_id, period_type, period_start)
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private EvaluationPeriod periodType;

    // Start date of this period
    // DAILY: the day itself
    // MONTHLY: first of month
    // QUARTERLY: first of quarter
    // YEARLY: Jan 1
    // CUSTOM: computed from customPeriodDays
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    // End date of this period (inclusive)
    // DAILY: same as periodStart
    // MONTHLY: last day of month
    // QUARTERLY: last day of quarter
    // YEARLY: Dec 31
    // CUSTOM: periodStart + customPeriodDays - 1
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "activities_logged", nullable = false)
    private Integer activitiesLogged = 0;

    // Snapshot of targetPerPeriod at time of last update
    @Column(name = "target_per_period")
    private Integer targetPerPeriod;

    // min(100, activitiesLogged / targetPerPeriod * 100)
    @Column(name = "consistency_score")
    private Double consistencyScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.activitiesLogged == null)
            this.activitiesLogged = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public GoalPeriodSnapshot() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public EvaluationPeriod getPeriodType() { return periodType; }
    public void setPeriodType(EvaluationPeriod periodType) 
        { this.periodType = periodType; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) 
        { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) 
        { this.periodEnd = periodEnd; }

    public Integer getActivitiesLogged() { return activitiesLogged; }
    public void setActivitiesLogged(Integer activitiesLogged) 
        { this.activitiesLogged = activitiesLogged; }

    public Integer getTargetPerPeriod() { return targetPerPeriod; }
    public void setTargetPerPeriod(Integer targetPerPeriod) 
        { this.targetPerPeriod = targetPerPeriod; }

    public Double getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(Double consistencyScore) 
        { this.consistencyScore = consistencyScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) 
        { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) 
        { this.updatedAt = updatedAt; }
}
