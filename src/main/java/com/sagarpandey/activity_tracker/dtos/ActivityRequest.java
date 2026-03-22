package com.sagarpandey.activity_tracker.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

public class ActivityRequest {

    @NotNull(message = "Data is required")
    private DataPayload data;

    public static class DataPayload {

        @NotBlank(message = "Activity name is required")
        private String name;

        @NotNull(message = "Activity start time is required")
        private OffsetDateTime startTime;

        @NotNull(message = "Activity end time is required")
        private OffsetDateTime endTime;

        private String description;

        // Domain, Subdomain, and Specific selection
        @NotBlank(message = "Domain ID is required")
        private String domainId;
        
        @NotBlank(message = "Domain name is required")
        private String domainName;
        
        @NotBlank(message = "Subdomain ID is required")
        private String subdomainId;
        
        @NotBlank(message = "Subdomain name is required")
        private String subdomainName;
        
        @NotBlank(message = "Specific ID is required")
        private String specificId;
        
        @NotBlank(message = "Specific name is required")
        private String specificName;

        private Integer mood;   // out of 5
        private Integer rating; // out of 5
        
        // Optional source field - will be set automatically if not provided
        private String source;  // e.g., "API_SINGLE", "API_BULK", "IMPORT", "MANUAL"

        // Optional — null means not linked to a goal
        private Long goalId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public OffsetDateTime getStartTime() { return startTime; }
        public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

        public OffsetDateTime getEndTime() { return endTime; }
        public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

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

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public Long getGoalId() { return goalId; }
        public void setGoalId(Long goalId) { this.goalId = goalId; }
    }

    public DataPayload getData() { return data; }
    public void setData(DataPayload data) { this.data = data; }
}
