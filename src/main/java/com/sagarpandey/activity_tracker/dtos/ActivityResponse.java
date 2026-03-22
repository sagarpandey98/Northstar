package com.sagarpandey.activity_tracker.dtos;

import java.time.OffsetDateTime;

public class ActivityResponse {
    private String name;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String description;
    private long id;
    private String created_at;
    
    // Domain, Subdomain, and Specific information
    private String domainId;
    private String domainName;
    private String subdomainId;
    private String subdomainName;
    private String specificId;
    private String specificName;
    
    // Rating and mood
    private Integer mood;
    private Integer rating;
    
    // Duration
    private String duration;
    
    // Source of activity creation
    private String source;

    // Null if activity is not linked to a goal
    private Long goalId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }

    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }

    public String getSubdomainId() { return subdomainId; }
    public void setSubdomainId(String subdomainId) { this.subdomainId = subdomainId; }

    public String getSubdomainName() { return subdomainName; }
    public void setSubdomainName(String subdomainName) { this.subdomainName = subdomainName; }

    public String getSpecificId() { return specificId; }
    public void setSpecificId(String specificId) { this.specificId = specificId; }

    public String getSpecificName() { return specificName; }
    public void setSpecificName(String specificName) { this.specificName = specificName; }

    public Integer getMood() { return mood; }
    public void setMood(Integer mood) { this.mood = mood; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }
}