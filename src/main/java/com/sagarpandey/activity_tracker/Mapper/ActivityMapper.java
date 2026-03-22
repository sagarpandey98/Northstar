package com.sagarpandey.activity_tracker.Mapper;

import com.sagarpandey.activity_tracker.dtos.ActivityRequest;
import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.enums.ActivitySource;
import com.sagarpandey.activity_tracker.models.Activity;
import com.sagarpandey.activity_tracker.Exceptions.DurationCalculationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class ActivityMapper {

    // Convert ActivityRequest to Activity entity
    public Activity toEntity(ActivityRequest activityRequest) throws DurationCalculationException {
        ActivityRequest.DataPayload data = activityRequest.getData();
        Activity activity = new Activity();
        activity.setName(data.getName());
        activity.setStartTime(data.getStartTime());
        activity.setEndTime(data.getEndTime());
        activity.setDuration(calculateDuration(data.getStartTime(), data.getEndTime()));
        activity.setDescription(data.getDescription());
        
        // Set domain/subdomain/specific information directly
        activity.setDomainId(data.getDomainId());
        activity.setDomainName(data.getDomainName());
        activity.setSubdomainId(data.getSubdomainId());
        activity.setSubdomainName(data.getSubdomainName());
        activity.setSpecificId(data.getSpecificId());
        activity.setSpecificName(data.getSpecificName());
        
        // Handle null mood and rating with default values
        activity.setMood(data.getMood() != null ? data.getMood() : 0);
        activity.setRating(data.getRating() != null ? data.getRating() : 0);
        
        // Set source - use provided source or default to API_SINGLE
        if (data.getSource() != null && !data.getSource().trim().isEmpty()) {
            activity.setSource(data.getSource());
        } else {
            activity.setSource(ActivitySource.API_SINGLE.getCode());
        }

        activity.setGoalId(data.getGoalId());
        
        return activity;
    }

    // Convert Activity entity to ActivityResponse DTO
    public ActivityResponse toResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setName(activity.getName());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setDescription(activity.getDescription());
        response.setDuration(activity.getDuration());
        response.setCreated_at(activity.getCreatedAt().toString());
        
        // Set domain information directly from the fields
        response.setDomainId(activity.getDomainId());
        response.setDomainName(activity.getDomainName());
        response.setSubdomainId(activity.getSubdomainId());
        response.setSubdomainName(activity.getSubdomainName());
        response.setSpecificId(activity.getSpecificId());
        response.setSpecificName(activity.getSpecificName());
        
        // Set mood and rating
        response.setMood(activity.getMood());
        response.setRating(activity.getRating());
        
        // Set source
        response.setSource(activity.getSource());

        response.setGoalId(activity.getGoalId());
        
        return response;
    }

    // Utility method to calculate the duration between start and end time
    private String calculateDuration(OffsetDateTime startTime, OffsetDateTime endTime) throws DurationCalculationException {
        if (startTime != null && endTime != null) {
            try {
                Duration duration = Duration.between(startTime, endTime);
                long seconds = duration.getSeconds();
                long hours = seconds / 3600;
                long minutes = (seconds % 3600) / 60;
                return String.format("%d hours, %d minutes", hours, minutes);
            } catch (Exception e) {
                throw new DurationCalculationException("Error calculating duration", e);
            }
        }
        return "0 hours, 0 minutes";
    }
}
