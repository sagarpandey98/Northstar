package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Exceptions.DurationCalculationException;
import com.sagarpandey.activity_tracker.Exceptions.ErrorWhileProcessing;
import com.sagarpandey.activity_tracker.Mapper.ActivityMapper;
import com.sagarpandey.activity_tracker.Repository.ActivityRepository;
import com.sagarpandey.activity_tracker.Repository.ActivitySpecifications;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Inteface.ActivityServiceInterface;
import com.sagarpandey.activity_tracker.Service.Inteface.GoalHealthService;
import com.sagarpandey.activity_tracker.dtos.ActivityRequest;
import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.dtos.ActivitySearchRequest;
import com.sagarpandey.activity_tracker.dtos.ActivitySearchResponse;
import com.sagarpandey.activity_tracker.dtos.ActivityBulkCreateRequest;
import com.sagarpandey.activity_tracker.dtos.ActivityBulkCreateResponse;
import com.sagarpandey.activity_tracker.enums.ActivitySource;
import com.sagarpandey.activity_tracker.Exceptions.ValidationException;
import com.sagarpandey.activity_tracker.models.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service("V1")
public class ActivityServiceV1 implements ActivityServiceInterface {
    private static final Logger log =
        LoggerFactory.getLogger(ActivityServiceV1.class);
    
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final GoalRepository goalRepository;
    @Autowired
    private GoalHealthService goalHealthService;

    public ActivityServiceV1(ActivityRepository activityRepository, ActivityMapper activityMapper, GoalRepository goalRepository) {
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
        this.goalRepository = goalRepository;
    }
    
    @Autowired
    public void setGoalHealthService(GoalHealthService goalHealthService) {
        this.goalHealthService = goalHealthService;
    }

    @Override
    public Activity create(HashMap<String, String> activityInfo) {
        Activity activity = new Activity();
        activity.setName(activityInfo.get("name"));
        activity.setStartTime(OffsetDateTime.parse(activityInfo.get("startTime")));
        activity.setDuration(activityInfo.get("duration"));
        activity.setEndTime(OffsetDateTime.parse(activityInfo.get("endTime")));
        activity.setDescription(activityInfo.get("description"));
        
        // Set domain/subdomain/specific information (UUIDs as strings)
        activity.setDomainId(activityInfo.get("domainId"));
        activity.setDomainName(activityInfo.get("domainName"));
        activity.setSubdomainId(activityInfo.get("subdomainId"));
        activity.setSubdomainName(activityInfo.get("subdomainName"));
        activity.setSpecificId(activityInfo.get("specificId"));
        activity.setSpecificName(activityInfo.get("specificName"));
        
        activity.setMood(Integer.parseInt(activityInfo.get("mood")));
        activity.setRating(Integer.parseInt(activityInfo.get("rating")));
        return activityRepository.save(activity);
    }

    @Override
    public Activity read(Long id) {
        Optional<Activity> activity = activityRepository.findById(id);
        return activity.orElse(null);
    }

    @Override
    public List<ActivityResponse> readAll() {
        List<Activity> activities = activityRepository.findAll();
        List<ActivityResponse> responses = new ArrayList<>();
        for (Activity activity : activities) {
            responses.add(activityMapper.toResponse(activity));
        }
        return responses;
    }

    public List<ActivityResponse> readAll(String userId) {
        List<Activity> activities = activityRepository.findAllByUserId(userId);
        List<ActivityResponse> responses = new ArrayList<>();
        for (Activity activity : activities) {
            responses.add(activityMapper.toResponse(activity));
        }
        return responses;
    }

    @Override
    public void update(HashMap<String, String> activityInfo) {
        try {
            Long id = Long.parseLong(activityInfo.get("id"));
            Optional<Activity> optionalActivity = activityRepository.findById(id);
            if (optionalActivity.isPresent()) {
                Activity activity = optionalActivity.get();
                activity.setName(activityInfo.get("name"));
                activity.setStartTime(OffsetDateTime.parse(activityInfo.get("startTime")));
                activity.setDuration(activityInfo.get("duration"));
                activity.setEndTime(OffsetDateTime.parse(activityInfo.get("endTime")));
                activity.setDescription(activityInfo.get("description"));
                activityRepository.save(activity);
            }
        } catch (Exception e) {
            // Log error if needed
        }
    }

    @Override
    public void delete(Long id) {
        try {
            activityRepository.deleteById(id);
        } catch (Exception e) {
            // Log error if needed
        }
    }

    public ActivityResponse createActivity(ActivityRequest request, String userId) throws ErrorWhileProcessing {
        try {
            // Debug logging
            System.out.println("=== ActivityServiceV1.createActivity Debug ===");
            System.out.println("Received userId: " + userId);
            System.out.println("Request data: " + request.getData());
            System.out.println("Goal ID from request: " + (request.getData() != null ? request.getData().getGoalId() : "null"));
            System.out.println("=============================================");
            
            if (request.getData() != null && request.getData().getGoalId() != null) {
                System.out.println("Checking goal existence for goalId: " + request.getData().getGoalId() + " and userId: " + userId);
                boolean goalExists = goalRepository.existsByIdAndUserId(request.getData().getGoalId(), userId);
                System.out.println("Goal exists result: " + goalExists);
                if (!goalExists) {
                    throw new ValidationException(
                        "Goal not found with id: " + request.getData().getGoalId() + " for current user"
                    );
                }
            }
            // Convert DTO to entity using mapper
            Activity activity = activityMapper.toEntity(request);

            // Set audit fields
            activity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            activity.setLastUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

            // Set userId from authentication token
            activity.setUserId(userId);
            System.out.println("User ID" + activity.getUserId());

            // Save the activity to the repository
            Activity savedActivity = activityRepository.save(activity);
            System.out.println(savedActivity.getId());

            // === PHASE 5 — Health score update ===
            try {
                if (savedActivity.getGoalId() != null) {
                    goalHealthService.onActivityLogged(
                        savedActivity.getGoalId(),
                        savedActivity.getStartTime().toLocalDate()
                    );
                }
            } catch (Exception e) {
                // Non-blocking — do not fail activity creation
                log.warn("Health update failed for activity {}: {}",
                    savedActivity.getId(), e.getMessage());
            }
            // === END PHASE 5 ===

            // Convert the saved Activity entity to a response DTO
            return activityMapper.toResponse(savedActivity);
        } catch (DurationCalculationException | DataAccessException | NullPointerException e) {
            System.out.println("Caught DurationCalculationException | DataAccessException | NullPointerException:");
            System.out.println("Exception type: " + e.getClass().getName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            throw new ErrorWhileProcessing("Error while processing activity data", e);
        } catch (Exception e) {
            System.out.println("Caught unexpected Exception:");
            System.out.println("Exception type: " + e.getClass().getName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            throw new ErrorWhileProcessing("An unexpected error occurred", e);
        }
    }

    public ActivityBulkCreateResponse createActivitiesBulk(ActivityBulkCreateRequest bulkRequest, String userId) {
        ActivityBulkCreateResponse response = new ActivityBulkCreateResponse();
        List<ActivityResponse> successful = new ArrayList<>();
        List<ActivityBulkCreateResponse.ActivityError> failed = new ArrayList<>();
        
        int totalRequested = bulkRequest.getActivities().size();
        
        for (ActivityRequest.DataPayload activityData : bulkRequest.getActivities()) {
            try {
                // Create ActivityRequest wrapper for the data
                ActivityRequest request = new ActivityRequest();
                
                // Set source for bulk creation if not already specified
                if (activityData.getSource() == null || activityData.getSource().isEmpty()) {
                    activityData.setSource(ActivitySource.API_BULK.getCode());
                }
                
                request.setData(activityData);
                
                // Create activity using existing method
                ActivityResponse activityResponse = createActivity(request, userId);
                
                // === PHASE 5 — Health score update ===
                try {
                    if (activityResponse.getGoalId() != null) {
                        goalHealthService.onActivityLogged(
                            activityResponse.getGoalId(),
                            activityResponse.getStartTime().toLocalDate()
                        );
                    }
                } catch (Exception e) {
                    log.warn("Health update failed for activity {}: {}",
                        activityResponse.getId(), e.getMessage());
                }
                // === END PHASE 5 ===
                
                successful.add(activityResponse);
                
            } catch (Exception e) {
                // Handle individual activity creation failure
                ActivityBulkCreateResponse.ActivityError error = new ActivityBulkCreateResponse.ActivityError();
                error.setActivityData(activityData);
                error.setErrorMessage(e.getMessage());
                error.setErrorCode("CREATION_FAILED");
                failed.add(error);
            }
        }
        
        // Set response data
        response.setSuccessful(successful);
        response.setFailed(failed);
        response.setTotalRequested(totalRequested);
        response.setTotalSuccessful(successful.size());
        response.setTotalFailed(failed.size());
        
        return response;
    }

    public List<ActivityResponse> getActivitiesByTimeRange(String userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<Activity> activities = activityRepository.findByStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndUserId(startTime, endTime, userId);
        List<ActivityResponse> responses = new ArrayList<>();
        for (Activity activity : activities) {
            responses.add(activityMapper.toResponse(activity));
        }
        return responses;
    }

    public ActivitySearchResponse searchActivities(String userId, ActivitySearchRequest searchRequest) {
        // Set default values if pagination is null
        if (searchRequest.getPagination() == null) {
            searchRequest.setPagination(new ActivitySearchRequest.PaginationCriteria());
        }
        
        ActivitySearchRequest.PaginationCriteria pagination = searchRequest.getPagination();
        
        // Create Sort object based on pagination criteria
        Sort.Direction direction = "ASC".equalsIgnoreCase(pagination.getSortDirection()) 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, pagination.getSortBy());
        
        // Create Pageable object
        Pageable pageable = PageRequest.of(pagination.getPage(), pagination.getSize(), sort);
        
        // Build specification for filtering
        Specification<Activity> spec = ActivitySpecifications.buildSpecification(userId, searchRequest.getFilter());
        
        // Execute query with pagination and filtering
        Page<Activity> activityPage = activityRepository.findAll(spec, pageable);
        
        // Convert to response DTOs
        List<ActivityResponse> activityResponses = new ArrayList<>();
        for (Activity activity : activityPage.getContent()) {
            activityResponses.add(activityMapper.toResponse(activity));
        }
        
        // Build response with pagination info
        ActivitySearchResponse response = new ActivitySearchResponse();
        response.setActivities(activityResponses);
        
        ActivitySearchResponse.PaginationInfo paginationInfo = new ActivitySearchResponse.PaginationInfo();
        paginationInfo.setCurrentPage(activityPage.getNumber());
        paginationInfo.setPageSize(activityPage.getSize());
        paginationInfo.setTotalElements(activityPage.getTotalElements());
        paginationInfo.setTotalPages(activityPage.getTotalPages());
        paginationInfo.setHasNext(activityPage.hasNext());
        paginationInfo.setHasPrevious(activityPage.hasPrevious());
        
        response.setPagination(paginationInfo);
        return response;
    }
}
