package com.sagarpandey.activity_tracker.controllers;

import com.sagarpandey.activity_tracker.Service.Interface.GoalHealthService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodService;
import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.GoalStatsResponse;
import com.sagarpandey.activity_tracker.dtos.ResponseWrapper;
import com.sagarpandey.activity_tracker.models.Goal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/goals")
@CrossOrigin(origins = "*")
public class GoalController {
    
    @Autowired
    private GoalService goalService;
    
    @Autowired
    private GoalHealthService goalHealthService;
    
    @Autowired
    private GoalPeriodService goalPeriodService;
    
    /**
     * Extract userId from JWT token
     */
    private String extractUserIdFromJwt(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("id"); // or whatever claim contains the user ID
        }
        throw new RuntimeException("User not authenticated");
    }
    
    /**
     * POST /api/v1/goals → create goal
     */
    @PostMapping
    public ResponseEntity<ResponseWrapper> createGoal(
            @Valid @RequestBody GoalRequest request,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        GoalResponse response = goalService.createGoal(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseWrapper("Goal created successfully", "success", response));
    }
    
    /**
     * GET /api/v1/goals → list all goals for user
     */
    @GetMapping
    public ResponseEntity<ResponseWrapper> getAllGoals(Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> goals = goalService.getAllGoalsByUser(userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Goals retrieved successfully", "success", goals));
    }
    
    /**
     * GET /api/v1/goals/{id} → fetch goal by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper> getGoalById(
            @PathVariable Long id,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        GoalResponse goal = goalService.getGoalById(id, userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Goal retrieved successfully", "success", goal));
    }
    
    /**
     * PUT /api/v1/goals/{id} → update goal
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseWrapper> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        GoalResponse response = goalService.updateGoal(id, request, userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Goal updated successfully", "success", response));
    }
    
    /**
     * DELETE /api/v1/goals/{id} → delete goal
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseWrapper> deleteGoal(
            @PathVariable Long id,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        goalService.deleteGoal(id, userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Goal deleted successfully", "success", null));
    }
    
    /**
     * GET /api/v1/goals/tree → fetch hierarchical structure
     */
    @GetMapping("/tree")
    public ResponseEntity<ResponseWrapper> getGoalTree(Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> goalTree = goalService.getGoalTree(userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Goal tree retrieved successfully", "success", goalTree));
    }
    
    /**
     * PATCH /api/v1/goals/progress/bulk → bulk progress update
     */
    @PatchMapping("/progress/bulk")
    public ResponseEntity<ResponseWrapper> updateProgressBulk(
            @RequestBody Map<Long, Double> progressUpdates,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> updatedGoals = goalService.updateProgressBulk(progressUpdates, userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Progress updated successfully", "success", updatedGoals));
    }
    
    /**
     * PATCH /api/v1/goals/status/bulk → bulk status update
     */
    @PatchMapping("/status/bulk")
    public ResponseEntity<ResponseWrapper> updateStatusBulk(
            @RequestBody Map<Long, Goal.Status> statusUpdates,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> updatedGoals = goalService.updateStatusBulk(statusUpdates, userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Status updated successfully", "success", updatedGoals));
    }
    
    /**
     * GET /api/v1/goals/statistics → get dashboard stats
     */
    @GetMapping("/statistics")
    public ResponseEntity<ResponseWrapper> getGoalStatistics(Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        GoalStatsResponse stats = goalService.getGoalStatistics(userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Statistics retrieved successfully", "success", stats));
    }
    
    /**
     * GET /api/v1/goals/overdue → get overdue goals
     */
    @GetMapping("/overdue")
    public ResponseEntity<ResponseWrapper> getOverdueGoals(Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> overdueGoals = goalService.getOverdueGoals(userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Overdue goals retrieved successfully", "success", overdueGoals));
    }
    
    /**
     * GET /api/v1/goals/due-soon → get upcoming goals
     */
    @GetMapping("/due-soon")
    public ResponseEntity<ResponseWrapper> getDueSoonGoals(Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> dueSoonGoals = goalService.getDueSoonGoals(userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Due soon goals retrieved successfully", "success", dueSoonGoals));
    }
    
    /**
     * GET /api/v1/goals/search?query= → text search
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseWrapper> searchGoals(
            @RequestParam String query,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> results = goalService.searchGoals(query, userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Search completed successfully", "success", results));
    }
    
    /**
     * GET /api/v1/goals/milestones → get milestone goals
     */
    @GetMapping("/milestones")
    public ResponseEntity<ResponseWrapper> getMilestones(Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        List<GoalResponse> milestones = goalService.getMilestones(userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Milestones retrieved successfully", "success", milestones));
    }
    
    /**
     * PATCH /api/v1/goals/{id}/progress → update specific goal progress
     */
    @PatchMapping("/{id}/progress")
    public ResponseEntity<ResponseWrapper> updateGoalProgress(
            @PathVariable Long id,
            @RequestBody Map<String, Double> progressData,
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        Double currentValue = progressData.get("currentValue");
        
        if (currentValue == null) {
            throw new RuntimeException("Current value is required");
        }
        
        GoalResponse updatedGoal = goalService.updateProgress(id, currentValue, userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Goal progress updated successfully", "success", updatedGoal));
    }
    
    /**
     * POST /api/v1/goals/recalculate → recalculate all progress for user
     */
    @PostMapping("/recalculate")
    public ResponseEntity<ResponseWrapper> recalculateAllProgress(Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        goalService.recalculateAllProgress(userId);
        
        return ResponseEntity.ok(
                new ResponseWrapper("Progress recalculated successfully", "success", null));
    }
    
    // === NEW ENDPOINTS - PHASE 7 ===
    
    // --- Health Summary ---
    @GetMapping("/health-summary")
    public ResponseEntity<?> getHealthSummary(
            Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("id");
        // Match the claim key used in existing endpoints above

        List<GoalResponse> goals =
            goalService.getHealthSummary(userId);

        return ResponseEntity.ok(
            new ResponseWrapper(
                "Health summary retrieved successfully",
                "success",
                goals
            )
        );
    }

    // --- Manual Health Recalculate ---
    @PatchMapping("/{id}/health/recalculate")
    public ResponseEntity<?> recalculateGoalHealth(
            @PathVariable Long id,
            Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("id");
        // Match the claim key used in existing endpoints above

        GoalResponse response =
            goalService.recalculateGoalHealth(id, userId);

        return ResponseEntity.ok(
            new ResponseWrapper(
                "Goal health recalculated successfully",
                "success",
                response
            )
        );
    }
}
