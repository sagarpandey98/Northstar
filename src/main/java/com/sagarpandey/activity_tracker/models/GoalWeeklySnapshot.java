/*
 * PHASE 4 - TABLE CREATED: goal_weekly_snapshots
 *
 * goal_id                  BIGINT        NOT NULL
 * user_id                  VARCHAR(255)  NOT NULL
 * week_start               DATE          NOT NULL
 * activities_logged        INTEGER       NOT NULL DEFAULT 0
 * target_frequency_weekly  INTEGER       NULLABLE
 * consistency_score_for_week DOUBLE PRECISION NULLABLE
 * created_at               TIMESTAMP
 * updated_at               TIMESTAMP
 *
 * UNIQUE CONSTRAINT: (goal_id, week_start)
 * One record per goal per week.
 * week_start is always a Monday.
 */

package com.sagarpandey.activity_tracker.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "goal_weekly_snapshots",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_goal_week",
            columnNames = {"goal_id", "week_start"}
        )
    },
    indexes = {
        @Index(
            name = "idx_snapshot_goal_week",
            columnList = "goal_id, week_start"
        ),
        @Index(
            name = "idx_snapshot_user_week",
            columnList = "user_id, week_start"
        ),
        @Index(
            name = "idx_snapshot_goal_id",
            columnList = "goal_id"
        )
    }
)
public class GoalWeeklySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK to Goal.id — stored as plain Long, no @ManyToOne
    // to avoid circular loading
    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    // userId copied from Goal for quick filtering
    // without joining goals table
    @Column(name = "user_id", nullable = false)
    private String userId;

    // Always the Monday of the week this snapshot covers
    // Use WeekUtils.getMondayOf(date) to compute this
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    // How many activities linked to this goal were logged
    // during this week. Incremented on each activity save.
    @Column(name = "activities_logged", nullable = false)
    private Integer activitiesLogged = 0;

    // Snapshot of targetFrequencyWeekly at time of calculation
    // Stored here so momentum can be calculated even if the
    // goal's target changes later
    @Column(name = "target_frequency_weekly")
    private Integer targetFrequencyWeekly;

    // Consistency score for this specific week
    // = min(100, activitiesLogged / targetFrequencyWeekly * 100)
    // Null if targetFrequencyWeekly was null or 0
    @Column(name = "consistency_score_for_week")
    private Double consistencyScoreForWeek;

    // Timestamp when this snapshot was first created
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Timestamp when this snapshot was last updated
    // Updated every time activitiesLogged is incremented
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Auto-set timestamps
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.activitiesLogged == null) {
            this.activitiesLogged = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Default constructor
    public GoalWeeklySnapshot() {}

    // Getters and setters — manual, no Lombok
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) 
        { this.weekStart = weekStart; }

    public Integer getActivitiesLogged() { return activitiesLogged; }
    public void setActivitiesLogged(Integer activitiesLogged) 
        { this.activitiesLogged = activitiesLogged; }

    public Integer getTargetFrequencyWeekly() 
        { return targetFrequencyWeekly; }
    public void setTargetFrequencyWeekly(Integer targetFrequencyWeekly) 
        { this.targetFrequencyWeekly = targetFrequencyWeekly; }

    public Double getConsistencyScoreForWeek() 
        { return consistencyScoreForWeek; }
    public void setConsistencyScoreForWeek(Double consistencyScoreForWeek) 
        { this.consistencyScoreForWeek = consistencyScoreForWeek; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) 
        { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) 
        { this.updatedAt = updatedAt; }
}
