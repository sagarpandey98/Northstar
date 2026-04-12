package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.GoalStatsResponse;
import com.sagarpandey.activity_tracker.models.Goal;

import java.util.List;
import java.util.Map;

public interface GoalService {
    
    // CRUD operations
    GoalResponse createGoal(GoalRequest request, String userId);
    GoalResponse getGoalById(Long id, String userId);
    GoalResponse getGoalByUuid(String uuid, String userId);
    List<GoalResponse> getAllGoalsByUser(String userId);
    GoalResponse updateGoal(Long id, GoalRequest request, String userId);
    void deleteGoal(Long id, String userId);
    
    // Tree operations
    List<GoalResponse> getGoalTree(String userId);
    List<GoalResponse> getChildGoals(String parentGoalId, String userId);
    
    // Bulk operations
    List<GoalResponse> updateProgressBulk(Map<Long, Double> progressUpdates, String userId);
    List<GoalResponse> updateStatusBulk(Map<Long, Goal.Status> statusUpdates, String userId);
    
    // Analytics and reporting
    GoalStatsResponse getGoalStatistics(String userId);
    List<GoalResponse> getOverdueGoals(String userId);
    List<GoalResponse> getDueSoonGoals(String userId);
    List<GoalResponse> getMilestones(String userId);
    
    // Search functionality
    List<GoalResponse> searchGoals(String query, String userId);
    
    // Progress tracking
    GoalResponse updateProgress(Long id, Double currentValue, String userId);
    void recalculateAllProgress(String userId);
    
    // === NEW METHODS - PHASE 7 ===
    
    /**
     * Returns all goals for user sorted by healthScore
     * ascending (worst health first).
     * Goals with null healthScore appear last.
     */
    List<GoalResponse> getHealthSummary(String userId);

    /**
     * Manually triggers health recalculation for a
     * specific goal and returns updated GoalResponse.
     */
    GoalResponse recalculateGoalHealth(Long id, String userId);
}
