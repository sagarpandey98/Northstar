package com.sagarpandey.activity_tracker.controllers;

import com.sagarpandey.activity_tracker.Exceptions.ErrorWhileProcessing;
import com.sagarpandey.activity_tracker.Service.V1.ActivityServiceV1;
import com.sagarpandey.activity_tracker.dtos.ActivityRequest;
import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.dtos.ActivitySearchRequest;
import com.sagarpandey.activity_tracker.dtos.ActivitySearchResponse;
import com.sagarpandey.activity_tracker.dtos.ActivityBulkCreateRequest;
import com.sagarpandey.activity_tracker.dtos.ActivityBulkCreateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
    private final ActivityServiceV1 activityServiceV1;

    public ActivityController(ActivityServiceV1 activityServiceV1) {
        this.activityServiceV1 = activityServiceV1;
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityRequest request,
            Authentication authentication) throws ErrorWhileProcessing {
        
        // Extract user ID from JWT token
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("id"); // or "user_id" depending on your JWT structure
        
        ActivityResponse response = activityServiceV1.createActivity(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<ActivityBulkCreateResponse> createActivitiesBulk(
            @Valid @RequestBody ActivityBulkCreateRequest request,
            Authentication authentication) {
        
        // Extract user ID from JWT token
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("id"); // or "user_id" depending on your JWT structure
        
        ActivityBulkCreateResponse response = activityServiceV1.createActivitiesBulk(request, userId);
        
        // Return 207 Multi-Status if there are both successes and failures
        if (response.getTotalFailed() > 0 && response.getTotalSuccessful() > 0) {
            return ResponseEntity.status(207).body(response); // Multi-Status
        }
        // Return 201 if all successful
        else if (response.getTotalFailed() == 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        // Return 400 if all failed
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getAllActivities(Authentication authentication) {
        // Extract user ID from JWT token
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("id"); // or "user_id" depending on your JWT structure
        
        List<ActivityResponse> response = activityServiceV1.readAll(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/search")
    public ResponseEntity<ActivitySearchResponse> searchActivities(
            @Valid @RequestBody ActivitySearchRequest searchRequest,
            Authentication authentication) {
        
        // Extract user ID from JWT token
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("id"); // or "user_id" depending on your JWT structure
        
        ActivitySearchResponse response = activityServiceV1.searchActivities(userId, searchRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}