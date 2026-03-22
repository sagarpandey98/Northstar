package com.sagarpandey.activity_tracker.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ActivityBulkCreateResponse {
    private List<ActivityResponse> successful;
    private List<ActivityError> failed;
    private int totalRequested;
    private int totalSuccessful;
    private int totalFailed;
    
    @Data
    public static class ActivityError {
        private ActivityRequest.DataPayload activityData;
        private String errorMessage;
        private String errorCode;
    }
}
