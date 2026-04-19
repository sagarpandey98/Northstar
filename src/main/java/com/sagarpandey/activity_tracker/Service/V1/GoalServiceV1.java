package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Exceptions.GoalNotFoundException;
import com.sagarpandey.activity_tracker.Exceptions.ValidationException;
import com.sagarpandey.activity_tracker.Mapper.GoalMapper;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Service.Interface.GoalService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalHealthService;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.GoalStatsResponse;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.Goal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoalServiceV1 implements GoalService {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private GoalMapper goalMapper;
    
    @Autowired
    private GoalHealthService goalHealthService;
    
    @Autowired
    private GoalPeriodRepository goalPeriodRepository;
    
    @Autowired
    private com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodService goalPeriodService;
    
    @Override
    public GoalResponse createGoal(GoalRequest request, String userId) {
        validateGoalRequest(request);
        
        // Validate parent goal exists if specified
        if (request.getParentGoalId() != null) {
            goalRepository.findByUuidAndUserIdAndIsDeletedFalse(request.getParentGoalId(), userId)
                    .orElseThrow(() -> new ValidationException("Parent goal not found"));
        }
        
        Goal goal = goalMapper.toEntity(request, userId);
        
        // DEBUG: Print Goal weights after mapping
        System.out.println("DEBUG: Goal weights after mapping - consistency: " + goal.getConsistencyWeight() + 
            ", momentum: " + goal.getMomentumWeight() + 
            ", progress: " + goal.getProgressWeight() + 
            ", goalType: " + goal.getGoalType());
        
        Goal savedGoal = goalRepository.save(goal);
        goalRepository.flush(); // Ensure entity is properly saved and accessible
        
        // DEBUG: Print savedGoal weights after save
        System.out.println("DEBUG: savedGoal weights after save - consistency: " + savedGoal.getConsistencyWeight() + 
            ", momentum: " + savedGoal.getMomentumWeight() + 
            ", progress: " + savedGoal.getProgressWeight() + 
            ", goalType: " + savedGoal.getGoalType());
        
        // Create the very first active Period for this Goal automatically
        // Only for trackable goals (not milestone goals)
        if (!savedGoal.getIsMilestone()) {
            GoalPeriod firstPeriod = goalPeriodService.createPeriodForGoal(savedGoal);
            goalPeriodRepository.save(firstPeriod);
        }
        
        return goalMapper.toResponse(savedGoal);
    }
    
    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id, String userId) {
        Goal goal = goalRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new GoalNotFoundException(id));
        
        return goalMapper.toResponse(goal);
    }
    
    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoalByUuid(String uuid, String userId) {
        Goal goal = goalRepository.findByUuidAndUserIdAndIsDeletedFalse(uuid, userId)
                .orElseThrow(() -> new GoalNotFoundException(uuid, userId));
        
        return goalMapper.toResponse(goal);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getAllGoalsByUser(String userId) {
        List<Goal> goals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        return goalMapper.toResponseList(goals);
    }
    
    @Override
    public GoalResponse updateGoal(Long id, GoalRequest request, String userId) {
        validateGoalRequest(request);
        
        Goal goal = goalRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new GoalNotFoundException(id));
        
        // Validate parent goal exists if specified and different from current goal
        if (request.getParentGoalId() != null && !request.getParentGoalId().equals(goal.getUuid())) {
            goalRepository.findByUuidAndUserIdAndIsDeletedFalse(request.getParentGoalId(), userId)
                    .orElseThrow(() -> new ValidationException("Parent goal not found"));
        }
        
        goalMapper.updateEntity(goal, request);
        Goal savedGoal = goalRepository.save(goal);
        
        return goalMapper.toResponse(savedGoal);
    }
    
    @Override
    public void deleteGoal(Long id, String userId) {
        Goal goal = goalRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new GoalNotFoundException(id));
        
        // Soft delete
        goal.setIsDeleted(true);
        goal.setLastUpdatedAt(LocalDateTime.now());
        goalRepository.save(goal);
        
        // Also soft delete all child goals
        List<Goal> childGoals = goalRepository.findByParentGoalIdAndUserIdAndIsDeletedFalse(goal.getUuid(), userId);
        for (Goal child : childGoals) {
            child.setIsDeleted(true);
            child.setLastUpdatedAt(LocalDateTime.now());
        }
        goalRepository.saveAll(childGoals);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalTree(String userId) {
        List<Goal> goals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        return goalMapper.buildGoalTree(goals);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getChildGoals(String parentGoalId, String userId) {
        List<Goal> childGoals = goalRepository.findByParentGoalIdAndUserIdAndIsDeletedFalse(parentGoalId, userId);
        return goalMapper.toResponseList(childGoals);
    }
    
    @Override
    public List<GoalResponse> updateProgressBulk(Map<Long, Double> progressUpdates, String userId) {
        List<Goal> goalsToUpdate = new ArrayList<>();
        
        for (Map.Entry<Long, Double> entry : progressUpdates.entrySet()) {
            Goal goal = goalRepository.findByIdAndUserIdAndIsDeletedFalse(entry.getKey(), userId)
                    .orElseThrow(() -> new GoalNotFoundException(entry.getKey()));
            
            goal.setCurrentValue(entry.getValue());
            goal.setProgressPercentage(goalMapper.calculateProgressPercentage(goal));
            goalMapper.updateStatusBasedOnProgress(goal);
            goal.setLastUpdatedAt(LocalDateTime.now());
            
            goalsToUpdate.add(goal);
        }
        
        List<Goal> savedGoals = goalRepository.saveAll(goalsToUpdate);
        return goalMapper.toResponseList(savedGoals);
    }
    
    @Override
    public List<GoalResponse> updateStatusBulk(Map<Long, Goal.Status> statusUpdates, String userId) {
        List<Goal> goalsToUpdate = new ArrayList<>();
        
        for (Map.Entry<Long, Goal.Status> entry : statusUpdates.entrySet()) {
            Goal goal = goalRepository.findByIdAndUserIdAndIsDeletedFalse(entry.getKey(), userId)
                    .orElseThrow(() -> new GoalNotFoundException(entry.getKey()));
            
            goal.setStatus(entry.getValue());
            if (entry.getValue() == Goal.Status.COMPLETED) {
                goal.setCompletedDate(LocalDateTime.now());
            }
            goal.setLastUpdatedAt(LocalDateTime.now());
            
            goalsToUpdate.add(goal);
        }
        
        List<Goal> savedGoals = goalRepository.saveAll(goalsToUpdate);
        return goalMapper.toResponseList(savedGoals);
    }
    
    @Override
    @Transactional(readOnly = true)
    public GoalStatsResponse getGoalStatistics(String userId) {
        List<Goal> allGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        GoalStatsResponse stats = new GoalStatsResponse();
        
        stats.setTotalGoals(allGoals.size());
        stats.setCompletedGoals((int) allGoals.stream().filter(g -> g.getStatus() == Goal.Status.COMPLETED).count());
        stats.setInProgressGoals((int) allGoals.stream().filter(g -> g.getStatus() == Goal.Status.IN_PROGRESS).count());
        stats.setNotStartedGoals((int) allGoals.stream().filter(g -> g.getStatus() == Goal.Status.NOT_STARTED).count());
        stats.setOverdueGoals((int) allGoals.stream().filter(g -> g.getStatus() == Goal.Status.OVERDUE).count());
        stats.setMilestones((int) allGoals.stream().filter(Goal::getIsMilestone).count());
        
        // Calculate due soon goals
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysFromNow = now.plusDays(7);
        stats.setDueSoonGoals((int) allGoals.stream()
                .filter(g -> g.getTargetDate() != null && 
                           g.getTargetDate().isAfter(now) && 
                           g.getTargetDate().isBefore(sevenDaysFromNow) &&
                           g.getStatus() != Goal.Status.COMPLETED)
                .count());
        
        // Calculate percentages
        if (stats.getTotalGoals() > 0) {
            stats.setOverallCompletionPercentage((double) stats.getCompletedGoals() / stats.getTotalGoals() * 100);
            stats.setOverduePercentage((double) stats.getOverdueGoals() / stats.getTotalGoals() * 100);
        }
        
        // Group by priority
        Map<Goal.Priority, Integer> priorityMap = allGoals.stream()
                .collect(Collectors.groupingBy(Goal::getPriority, 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
        stats.setGoalsByPriority(priorityMap);
        
        // Group by status
        Map<Goal.Status, Integer> statusMap = allGoals.stream()
                .collect(Collectors.groupingBy(Goal::getStatus, 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
        stats.setGoalsByStatus(statusMap);
        
        // === NEW STATS - PHASE 7 ===
        
        // Health status counts
        stats.setThrivingGoals((int) allGoals.stream()
            .filter(g -> g.getHealthStatus() == HealthStatus.THRIVING)
            .count());
        stats.setOnTrackGoals((int) allGoals.stream()
            .filter(g -> g.getHealthStatus() == HealthStatus.ON_TRACK)
            .count());
        stats.setAtRiskGoals((int) allGoals.stream()
            .filter(g -> g.getHealthStatus() == HealthStatus.AT_RISK)
            .count());
        stats.setCriticalGoals((int) allGoals.stream()
            .filter(g -> g.getHealthStatus() == HealthStatus.CRITICAL)
            .count());
        stats.setUntrackedGoals((int) allGoals.stream()
            .filter(g -> g.getHealthStatus() == null
                || g.getHealthStatus() == HealthStatus.UNTRACKED)
            .count());

        // Average health score
        OptionalDouble avgHealth = allGoals.stream()
            .filter(g -> g.getHealthScore() != null)
            .mapToDouble(Goal::getHealthScore)
            .average();
        stats.setAverageHealthScore(
            avgHealth.isPresent() ? avgHealth.getAsDouble() : null
        );

        // Tracked goals count
        stats.setTrackedGoals((int) allGoals.stream()
            .filter(g -> g.getScheduleSpec() != null)
            .count());

        // Leaf goals count (no children)
        stats.setLeafGoals((int) allGoals.stream()
            .filter(g -> goalRepository
                .findByParentGoalIdAndUserIdAndIsDeletedFalse(
                    g.getUuid(), userId)
                .isEmpty())
            .count());
        
        return stats;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getOverdueGoals(String userId) {
        List<Goal> overdueGoals = goalRepository.findOverdueGoals(userId, LocalDateTime.now());
        return goalMapper.toResponseList(overdueGoals);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getDueSoonGoals(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysFromNow = now.plusDays(7);
        List<Goal> dueSoonGoals = goalRepository.findDueSoonGoals(userId, now, sevenDaysFromNow);
        return goalMapper.toResponseList(dueSoonGoals);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getMilestones(String userId) {
        List<Goal> milestones = goalRepository.findByUserIdAndIsMilestoneTrueAndIsDeletedFalse(userId);
        return goalMapper.toResponseList(milestones);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> searchGoals(String query, String userId) {
        List<Goal> goals = goalRepository.searchGoals(userId, query);
        return goalMapper.toResponseList(goals);
    }
    
    @Override
    public GoalResponse updateProgress(Long id, Double currentValue, String userId) {
        Goal goal = goalRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new GoalNotFoundException(id));
        
        goal.setCurrentValue(currentValue);
        goal.setProgressPercentage(goalMapper.calculateProgressPercentage(goal));
        goalMapper.updateStatusBasedOnProgress(goal);
        goal.setLastUpdatedAt(LocalDateTime.now());
        
        Goal savedGoal = goalRepository.save(goal);
        return goalMapper.toResponse(savedGoal);
    }
    
    @Override
    public void recalculateAllProgress(String userId) {
        List<Goal> goals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
        
        for (Goal goal : goals) {
            goal.setProgressPercentage(goalMapper.calculateProgressPercentage(goal));
            goalMapper.updateStatusBasedOnProgress(goal);
            goal.setLastUpdatedAt(LocalDateTime.now());
        }
        
        goalRepository.saveAll(goals);
    }
    
    // === NEW METHODS - PHASE 7 ===
    
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getHealthSummary(String userId) {
        List<Goal> goals =
            goalRepository.findByUserIdAndIsDeletedFalse(userId);

        return goals.stream()
            .map(goalMapper::toResponse)
            .sorted((a, b) -> {
                // Null healthScore goes to end (least priority)
                if (a.getHealthScore() == null
                        && b.getHealthScore() == null) return 0;
                if (a.getHealthScore() == null) return 1;
                if (b.getHealthScore() == null) return -1;
                // Sort ascending — worst health first
                return Double.compare(
                    a.getHealthScore(), b.getHealthScore()
                );
            })
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public GoalResponse recalculateGoalHealth(
            Long id, String userId) {
        // Verify goal belongs to user
        Goal goal = goalRepository
            .findByIdAndUserIdAndIsDeletedFalse(id, userId)
            .orElseThrow(() ->
                new com.sagarpandey.activity_tracker
                    .Exceptions.GoalNotFoundException(id)
            );

        // Trigger health recalculation
        goalHealthService.updateGoalHealth(goal);

        // Fetch updated goal and return response
        Goal updatedGoal = goalRepository
            .findByIdAndUserIdAndIsDeletedFalse(id, userId)
            .orElseThrow(() ->
                new com.sagarpandey.activity_tracker
                    .Exceptions.GoalNotFoundException(id)
            );

        return goalMapper.toResponse(updatedGoal);
    }
    
    private void validateGoalRequest(GoalRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("Goal title is required");
        }
        
        if (request.getPriority() == null) {
            throw new ValidationException("Goal priority is required");
        }
        
        if (request.getMetric() == null) {
            throw new ValidationException("Goal metric is required");
        }
        
        if (request.getTargetOperator() == null) {
            throw new ValidationException("Goal target operator is required");
        }
        
        if (request.getTargetValue() == null || request.getTargetValue() < 0) {
            throw new ValidationException("Goal target value must be non-negative");
        }
        
        if (request.getCurrentValue() != null && request.getCurrentValue() < 0) {
            throw new ValidationException("Goal current value must be non-negative");
        }
        
        if (request.getTargetDate() != null && request.getStartDate() != null && 
            request.getTargetDate().isBefore(request.getStartDate())) {
            throw new ValidationException("Target date cannot be before start date");
        }

        // === NEW TIME BOUND LEDGER VALIDATIONS ===
        if (request.getScheduleSpec() != null) {
            if (request.getMinimumSessionPeriod() == null || request.getMinimumSessionPeriod() <= 0) {
                throw new ValidationException("minimumSessionPeriod is strongly recommended when tracking consistency");
            }
            if (request.getMaximumSessionPeriod() == null || request.getMaximumSessionPeriod() <= 0) {
                throw new ValidationException("maximumSessionPeriod is strongly recommended when tracking progress limits");
            }
        }
    }
}
