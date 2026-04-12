package com.sagarpandey.activity_tracker.controllers;

import com.sagarpandey.activity_tracker.Service.Interface.PriorityEngine;
import com.sagarpandey.activity_tracker.Service.V1.PriorityEngineV1;
import com.sagarpandey.activity_tracker.dtos.PriorityGoalResponse;
import com.sagarpandey.activity_tracker.dtos.ResponseWrapper;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/priority")
@CrossOrigin(origins = "*")
public class PriorityController {
    
    @Autowired
    private PriorityEngine priorityEngine;
    
    @Autowired
    private GoalRepository goalRepository;
    
    /**
     * Get all goals sorted by effective priority (highest first)
     */
    @GetMapping("/goals/sorted")
    public ResponseEntity<ResponseWrapper> getGoalsSortedByPriority(
            Authentication authentication) {
        
        String userId = ((Jwt) authentication.getPrincipal()).getSubject();
        
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Goal> sortedGoals = priorityEngine.getGoalsSortedByPriority(allGoals);
        
        List<PriorityGoalResponse> response = sortedGoals.stream()
            .map(goal -> {
                double score = priorityEngine.calculateEffectivePriorityScore(goal);
                String parentTitle = getParentGoalTitle(goal, userId);
                return new PriorityGoalResponse(goal, score, parentTitle);
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(
            new ResponseWrapper("Goals retrieved and sorted by priority", "success", response)
        );
    }
    
    /**
     * Get goals grouped by priority level
     */
    @GetMapping("/goals/grouped")
    public ResponseEntity<ResponseWrapper> getGoalsGroupedByPriority(
            Authentication authentication) {
        
        String userId = ((Jwt) authentication.getPrincipal()).getSubject();
        
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        Map<Goal.Priority, List<Goal>> groupedGoals = priorityEngine.getGoalsGroupedByPriority(allGoals);
        
        Map<Goal.Priority, List<PriorityGoalResponse>> response = groupedGoals.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(goal -> {
                        double score = priorityEngine.calculateEffectivePriorityScore(goal);
                        String parentTitle = getParentGoalTitle(goal, userId);
                        return new PriorityGoalResponse(goal, score, parentTitle);
                    })
                    .collect(Collectors.toList())
            ));
        
        return ResponseEntity.ok(
            new ResponseWrapper("Goals grouped by priority level", "success", response)
        );
    }
    
    /**
     * Get Today's Focus list - most important goals across all hierarchies
     */
    @GetMapping("/goals/today-focus")
    public ResponseEntity<ResponseWrapper> getTodaysFocus(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        String userId = ((Jwt) authentication.getPrincipal()).getSubject();
        
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Goal> focusGoals = priorityEngine.getTodaysFocus(allGoals, limit);
        
        List<PriorityGoalResponse> response = focusGoals.stream()
            .map(goal -> {
                double score = priorityEngine.calculateEffectivePriorityScore(goal);
                String parentTitle = getParentGoalTitle(goal, userId);
                return new PriorityGoalResponse(goal, score, parentTitle);
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(
            new ResponseWrapper("Today's focus goals retrieved", "success", response)
        );
    }
    
    /**
     * Get priority score for a specific goal
     */
    @GetMapping("/goals/{goalUuid}/score")
    public ResponseEntity<ResponseWrapper> getGoalPriorityScore(
            @PathVariable String goalUuid,
            Authentication authentication) {
        
        String userId = ((Jwt) authentication.getPrincipal()).getSubject();
        
        Goal goal = goalRepository.findByUuidAndUserIdAndIsDeletedFalse(goalUuid, userId)
            .orElseThrow(() -> new RuntimeException("Goal not found"));
        
        double effectiveScore = priorityEngine.calculateEffectivePriorityScore(goal);
        int rawPriorityValue = priorityEngine.getPriorityValue(goal.getPriority());
        
        Map<String, Object> response = new HashMap<>();
        response.put("goalUuid", goalUuid);
        response.put("title", goal.getTitle());
        response.put("priority", goal.getPriority());
        response.put("rawPriorityValue", rawPriorityValue);
        response.put("effectivePriorityScore", effectiveScore);
        response.put("parentGoalId", goal.getParentGoalId());
        response.put("parentGoalTitle", getParentGoalTitle(goal, userId));
        
        if (goal.getParentGoalId() != null) {
            Goal parentGoal = goalRepository.findByUuidAndUserIdAndIsDeletedFalse(
                goal.getParentGoalId(), userId
            ).orElse(null);
            
            if (parentGoal != null) {
                response.put("parentPriority", parentGoal.getPriority());
                response.put("parentPriorityValue", priorityEngine.getPriorityValue(parentGoal.getPriority()));
                
                // Show weighted calculation details
                double weightedScore = priorityEngine.calculateWeightedPriorityScore(
                    goal.getPriority(), parentGoal.getPriority(), 0.7, 0.3
                );
                response.put("weightedCalculation", Map.of(
                    "childWeight", 0.7,
                    "parentWeight", 0.3,
                    "weightedScore", weightedScore
                ));
            }
        }
        
        return ResponseEntity.ok(
            new ResponseWrapper("Priority score calculated", "success", response)
        );
    }
    
    /**
     * Get priority statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ResponseWrapper> getPriorityStatistics(
            Authentication authentication) {
        
        String userId = ((Jwt) authentication.getPrincipal()).getSubject();
        
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        
        Map<Goal.Priority, Long> priorityCounts = allGoals.stream()
            .collect(Collectors.groupingBy(Goal::getPriority, Collectors.counting()));
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalGoals", allGoals.size());
        response.put("priorityDistribution", priorityCounts);
        
        // Calculate average priority scores
        double averageScore = allGoals.stream()
            .mapToDouble(goal -> priorityEngine.calculateEffectivePriorityScore(goal))
            .average()
            .orElse(0.0);
        response.put("averagePriorityScore", averageScore);
        
        // Get top 5 priorities
        List<PriorityGoalResponse> topPriorities = priorityEngine.getTodaysFocus(allGoals, 5)
            .stream()
            .map(goal -> {
                double score = priorityEngine.calculateEffectivePriorityScore(goal);
                String parentTitle = getParentGoalTitle(goal, userId);
                return new PriorityGoalResponse(goal, score, parentTitle);
            })
            .collect(Collectors.toList());
        response.put("topPriorities", topPriorities);
        
        return ResponseEntity.ok(
            new ResponseWrapper("Priority statistics retrieved", "success", response)
        );
    }
    
    private String getParentGoalTitle(Goal goal, String userId) {
        if (goal.getParentGoalId() == null) {
            return null;
        }
        
        return goalRepository.findByUuidAndUserIdAndIsDeletedFalse(goal.getParentGoalId(), userId)
            .map(Goal::getTitle)
            .orElse(null);
    }
}
