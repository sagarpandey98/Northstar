package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.models.Goal;
import java.time.LocalDateTime;

/**
 * DTO for goal response with priority score information
 */
public class PriorityGoalResponse {
    
    private String uuid;
    private String title;
    private String description;
    private Goal.Priority priority;
    private String parentGoalId;
    private String parentGoalTitle;
    private double effectivePriorityScore;
    private Goal.Status status;
    private Double progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    
    // Constructors
    public PriorityGoalResponse() {}
    
    public PriorityGoalResponse(Goal goal, double effectivePriorityScore, String parentGoalTitle) {
        this.uuid = goal.getUuid();
        this.title = goal.getTitle();
        this.description = goal.getDescription();
        this.priority = goal.getPriority();
        this.parentGoalId = goal.getParentGoalId();
        this.parentGoalTitle = parentGoalTitle;
        this.effectivePriorityScore = effectivePriorityScore;
        this.status = goal.getStatus();
        this.progressPercentage = goal.getProgressPercentage();
        this.createdAt = goal.getCreatedAt();
        this.lastUpdatedAt = goal.getLastUpdatedAt();
    }
    
    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Goal.Priority getPriority() { return priority; }
    public void setPriority(Goal.Priority priority) { this.priority = priority; }
    
    public String getParentGoalId() { return parentGoalId; }
    public void setParentGoalId(String parentGoalId) { this.parentGoalId = parentGoalId; }
    
    public String getParentGoalTitle() { return parentGoalTitle; }
    public void setParentGoalTitle(String parentGoalTitle) { this.parentGoalTitle = parentGoalTitle; }
    
    public double getEffectivePriorityScore() { return effectivePriorityScore; }
    public void setEffectivePriorityScore(double effectivePriorityScore) { this.effectivePriorityScore = effectivePriorityScore; }
    
    public Goal.Status getStatus() { return status; }
    public void setStatus(Goal.Status status) { this.status = status; }
    
    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
