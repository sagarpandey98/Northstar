package com.sagarpandey.activity_tracker.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "activities",
    indexes = {
        @Index(name = "idx_activity_goal_id", columnList = "goal_id")
    }
)
public class Activity extends BaseModel {
    
    // Basic activity fields
    private String name;
    private OffsetDateTime startTime;
    private String duration;
    private OffsetDateTime endTime;
    private String description;
    
    // Domain information (stored as values, not relationships)
    private String domainName;
    private String subdomainName;
    private String specificName;
    
    // Domain IDs for reference (UUIDs stored as strings)
    private String domainId;
    private String subdomainId;
    private String specificId;
    
    // Subjective measurements
    @Column
    private int mood; // assuming mood is out of 5
    
    @Column
    private int rating; // assuming rating is out of 5
    
    // Source of activity creation
    @Column
    private String source; // e.g., "API_SINGLE", "API_BULK", "IMPORT", "MANUAL", etc.

    // === NEW FIELD - PHASE 3 ===

    @Column(name = "goal_id")
    private Long goalId;
    // Optional FK reference to Goal.id
    // Null means this activity is not linked to any goal
    // ON DELETE behavior: if goal is deleted, set this to null
    // DO NOT add @ManyToOne or @JoinColumn — keep it as a plain
    // Long to avoid circular loading issues. The health engine
    // will look up the goal separately when needed.

    /*
     * PHASE 3 - NEW COLUMN ADDED TO activities TABLE:
     * goal_id   BIGINT   NULLABLE
     * No FK constraint enforced at DB level intentionally
     * to avoid cascade issues. Integrity handled in service layer.
     */

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }

    public String getSubdomainName() { return subdomainName; }
    public void setSubdomainName(String subdomainName) { this.subdomainName = subdomainName; }

    public String getSpecificName() { return specificName; }
    public void setSpecificName(String specificName) { this.specificName = specificName; }

    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }

    public String getSubdomainId() { return subdomainId; }
    public void setSubdomainId(String subdomainId) { this.subdomainId = subdomainId; }

    public String getSpecificId() { return specificId; }
    public void setSpecificId(String specificId) { this.specificId = specificId; }

    public int getMood() { return mood; }
    public void setMood(int mood) { this.mood = mood; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }
}
