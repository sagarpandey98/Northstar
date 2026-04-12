package com.sagarpandey.activity_tracker.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Quick Log Request DTO for rapid activity logging
 * Pre-filled context from todo item for seamless logging
 */
public class QuickLogRequest {
    
    @NotNull(message = "Goal ID is required")
    private Long goalId;
    
    private String name;
    private String description;
    private String startTime;
    private String endTime;
    private String duration;
    
    // Completion details from todo interaction
    private Integer mood;           // 1-5 scale from quick complete
    private Integer rating;         // 1-5 scale from quick complete
    private Integer quantity;       // How much was completed (e.g., 5 problems)
    private Integer timeSpentMinutes; // Actual time spent
    
    // Context from todo
    private String todoContext;     // Pre-filled context from smart todo
    private String quickLogType;   // "COMPLETE", "PARTIAL", "SKIP"
    
    // Constructors
    public QuickLogRequest() {}
    
    public QuickLogRequest(Long goalId) {
        this.goalId = goalId;
    }
    
    // Getters and Setters
    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    
    public Integer getMood() { return mood; }
    public void setMood(Integer mood) { this.mood = mood; }
    
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getTimeSpentMinutes() { return timeSpentMinutes; }
    public void setTimeSpentMinutes(Integer timeSpentMinutes) { this.timeSpentMinutes = timeSpentMinutes; }
    
    public String getTodoContext() { return todoContext; }
    public void setTodoContext(String todoContext) { this.todoContext = todoContext; }
    
    public String getQuickLogType() { return quickLogType; }
    public void setQuickLogType(String quickLogType) { this.quickLogType = quickLogType; }
}
