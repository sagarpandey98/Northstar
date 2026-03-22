package com.sagarpandey.activity_tracker.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ActivityBulkCreateRequest {
    
    @NotEmpty(message = "Activities list cannot be empty")
    @Valid
    private List<ActivityRequest.DataPayload> activities;
}
