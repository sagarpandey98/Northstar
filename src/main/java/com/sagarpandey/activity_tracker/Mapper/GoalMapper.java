package com.sagarpandey.activity_tracker.Mapper;

import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Inteface.RollupService;
import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.utils.PeriodUtils;
import com.sagarpandey.activity_tracker.validators.GoalWeightValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GoalMapper {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    @Lazy
    private RollupService rollupService;
    
    /**
     * Convert GoalRequest to Goal entity
     */
    public Goal toEntity(GoalRequest request, String userId) {
        Goal goal = new Goal();
        goal.setUuid(UUID.randomUUID().toString());
        goal.setUserId(userId);
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setNotes(request.getNotes());
        goal.setPriority(request.getPriority());
        goal.setStatus(request.getStatus() != null ? request.getStatus() : Goal.Status.NOT_STARTED);
        goal.setMetric(request.getMetric());
        goal.setTargetOperator(request.getTargetOperator());
        goal.setTargetValue(request.getTargetValue());
        goal.setCurrentValue(request.getCurrentValue() != null ? request.getCurrentValue() : 0.0);
        goal.setStartDate(request.getStartDate());
        goal.setTargetDate(request.getTargetDate());
        goal.setParentGoalId(request.getParentGoalId());
        goal.setIsMilestone(request.getIsMilestone() != null ? request.getIsMilestone() : false);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setLastUpdatedAt(LocalDateTime.now());
        goal.setIsDeleted(false);

        // New fields - Phase 2
        goal.setGoalType(request.getGoalType());
        goal.setTargetFrequencyWeekly(request.getTargetFrequencyWeekly());
        goal.setTargetVolumeDaily(request.getTargetVolumeDaily());

        goal.setScheduleType(
                request.getScheduleType() != null
                        ? request.getScheduleType()
                        : goal.getDefaultScheduleType()
        );

        if (request.getScheduleDays() != null) {
            goal.setScheduleDaysList(request.getScheduleDays());
        }

        goal.setMinimumSessionMinutes(request.getMinimumSessionMinutes());

        goal.setAllowDoubleLogging(
                request.getAllowDoubleLogging() != null
                        ? request.getAllowDoubleLogging()
                        : Boolean.TRUE
        );

        goal.setMissesAllowedPerWeek(request.getMissesAllowedPerWeek());
        goal.setMissesAllowedPerMonth(request.getMissesAllowedPerMonth());

        GoalWeightValidator.validateWeights(
                request.getConsistencyWeight(),
                request.getMomentumWeight(),
                request.getProgressWeight()
        );

        if (request.getConsistencyWeight() != null) {
            goal.setConsistencyWeight(request.getConsistencyWeight());
            goal.setMomentumWeight(request.getMomentumWeight());
            goal.setProgressWeight(request.getProgressWeight());
        }
        
        // === PHASE 9 mappings ===
        goal.setEvaluationPeriod(request.getEvaluationPeriod());
        goal.setTargetPerPeriod(request.getTargetPerPeriod());
        goal.setCustomPeriodDays(request.getCustomPeriodDays());

        // Auto-set currentPeriodStart when evaluationPeriod is set
        if (request.getEvaluationPeriod() != null
                && request.getEvaluationPeriod()
                    != EvaluationPeriod.WEEKLY) {
            LocalDate periodStart = PeriodUtils.getPeriodStart(
                LocalDate.now(),
                request.getEvaluationPeriod(),
                request.getCustomPeriodDays(),
                null
            );
            goal.setCurrentPeriodStart(periodStart);
            goal.setCurrentPeriodCount(0);
        }
        
        // Calculate initial progress percentage
        goal.setProgressPercentage(calculateProgressPercentage(goal));
        
        return goal;
    }
    
    /**
     * Update existing Goal entity from GoalRequest
     */
    public void updateEntity(Goal goal, GoalRequest request) {
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setNotes(request.getNotes());
        goal.setPriority(request.getPriority());
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }
        goal.setMetric(request.getMetric());
        goal.setTargetOperator(request.getTargetOperator());
        goal.setTargetValue(request.getTargetValue());
        if (request.getCurrentValue() != null) {
            goal.setCurrentValue(request.getCurrentValue());
        }
        goal.setStartDate(request.getStartDate());
        goal.setTargetDate(request.getTargetDate());
        goal.setParentGoalId(request.getParentGoalId());
        if (request.getIsMilestone() != null) {
            goal.setIsMilestone(request.getIsMilestone());
        }
        goal.setLastUpdatedAt(LocalDateTime.now());

        // New fields - Phase 2
        goal.setGoalType(request.getGoalType());
        goal.setTargetFrequencyWeekly(request.getTargetFrequencyWeekly());
        goal.setTargetVolumeDaily(request.getTargetVolumeDaily());

        goal.setScheduleType(
                request.getScheduleType() != null
                        ? request.getScheduleType()
                        : goal.getDefaultScheduleType()
        );

        if (request.getScheduleDays() != null) {
            goal.setScheduleDaysList(request.getScheduleDays());
        }

        goal.setMinimumSessionMinutes(request.getMinimumSessionMinutes());

        goal.setAllowDoubleLogging(
                request.getAllowDoubleLogging() != null
                        ? request.getAllowDoubleLogging()
                        : Boolean.TRUE
        );

        goal.setMissesAllowedPerWeek(request.getMissesAllowedPerWeek());
        goal.setMissesAllowedPerMonth(request.getMissesAllowedPerMonth());

        GoalWeightValidator.validateWeights(
                request.getConsistencyWeight(),
                request.getMomentumWeight(),
                request.getProgressWeight()
        );

        if (request.getConsistencyWeight() != null) {
            goal.setConsistencyWeight(request.getConsistencyWeight());
            goal.setMomentumWeight(request.getMomentumWeight());
            goal.setProgressWeight(request.getProgressWeight());
        }
        
        // Recalculate progress percentage
        goal.setProgressPercentage(calculateProgressPercentage(goal));
        
        // Auto-update status based on progress and dates
        updateStatusBasedOnProgress(goal);
    }
    
    /**
     * Convert Goal entity to GoalResponse
     */
    public GoalResponse toResponse(Goal goal) {
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setUuid(goal.getUuid());
        response.setUserId(goal.getUserId());
        response.setTitle(goal.getTitle());
        response.setDescription(goal.getDescription());
        response.setNotes(goal.getNotes());
        response.setPriority(goal.getPriority());
        response.setStatus(goal.getStatus());
        response.setMetric(goal.getMetric());
        response.setTargetOperator(goal.getTargetOperator());
        response.setTargetValue(goal.getTargetValue());
        response.setCurrentValue(goal.getCurrentValue());
        response.setProgressPercentage(goal.getProgressPercentage());
        response.setStartDate(goal.getStartDate());
        response.setTargetDate(goal.getTargetDate());
        response.setCompletedDate(goal.getCompletedDate());
        response.setParentGoalId(goal.getParentGoalId());
        response.setIsMilestone(goal.getIsMilestone());
        response.setCreatedAt(goal.getCreatedAt());
        response.setLastUpdatedAt(goal.getLastUpdatedAt());

        // New fields - Phase 2
        response.setGoalType(goal.getGoalType());

        // isLeaf computed using RollupService
        response.setIsLeaf(
            rollupService.isLeafGoal(goal.getUuid(), goal.getUserId())
        );

        response.setIsTracked(
            (goal.getTargetFrequencyWeekly() != null
                && goal.getTargetFrequencyWeekly() > 0)
            ||
            (goal.getEvaluationPeriod() != null
                && goal.getTargetPerPeriod() != null
                && goal.getTargetPerPeriod() > 0)
        );

        response.setTargetFrequencyWeekly(goal.getTargetFrequencyWeekly());
        response.setTargetVolumeDaily(goal.getTargetVolumeDaily());

        response.setScheduleType(goal.getScheduleType());
        response.setScheduleDays(goal.getScheduleDaysList());
        response.setMinimumSessionMinutes(goal.getMinimumSessionMinutes());
        response.setAllowDoubleLogging(goal.getAllowDoubleLogging());

        response.setMissesAllowedPerWeek(goal.getMissesAllowedPerWeek());
        response.setMissesAllowedPerMonth(goal.getMissesAllowedPerMonth());

        response.setEffectiveConsistencyWeight(goal.getEffectiveConsistencyWeight());
        response.setEffectiveMomentumWeight(goal.getEffectiveMomentumWeight());
        response.setEffectiveProgressWeight(goal.getEffectiveProgressWeight());

        response.setConsistencyWeight(goal.getConsistencyWeight());
        response.setMomentumWeight(goal.getMomentumWeight());
        response.setProgressWeight(goal.getProgressWeight());

        response.setConsistencyScore(goal.getConsistencyScore());
        response.setMomentumScore(goal.getMomentumScore());
        response.setHealthScore(goal.getHealthScore());
        response.setHealthStatus(goal.getHealthStatus());

        // For parent goals, calculate rolled-up health score
        if (!response.getIsLeaf()) {
            List<Goal> children = goalRepository
                .findByParentGoalIdAndUserIdAndIsDeletedFalse(
                    goal.getUuid(), goal.getUserId()
                );
            Double rolledUpScore =
                rollupService.calculateRolledUpHealthScore(children);
            if (rolledUpScore != null) {
                response.setHealthScore(rolledUpScore);
            }
        } else {
            response.setHealthScore(goal.getHealthScore());
        }

        response.setCurrentStreak(goal.getCurrentStreak());
        response.setLongestStreak(goal.getLongestStreak());

        // parentInsights populated using RollupService
        if (!response.getIsLeaf()) {
            response.setParentInsights(
                rollupService.buildParentInsights(
                    goal.getId(), goal.getUserId()
                )
            );
        } else {
            response.setParentInsights(null);
        }

        // === PHASE 9 mappings ===
        response.setEvaluationPeriod(goal.getEvaluationPeriod());
        response.setTargetPerPeriod(goal.getTargetPerPeriod());
        response.setCustomPeriodDays(goal.getCustomPeriodDays());
        response.setCurrentPeriodStart(goal.getCurrentPeriodStart());
        response.setCurrentPeriodCount(goal.getCurrentPeriodCount());
        response.setPeriodConsistencyScore(
            goal.getPeriodConsistencyScore()
        );
        
        return response;
    }
    
    /**
     * Convert list of Goal entities to GoalResponse list
     */
    public List<GoalResponse> toResponseList(List<Goal> goals) {
        return goals.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Build hierarchical goal tree structure
     */
    public List<GoalResponse> buildGoalTree(List<Goal> goals) {
        // Convert all goals to responses
        List<GoalResponse> responses = toResponseList(goals);
        
        // Create a map for quick lookup by UUID
        Map<String, GoalResponse> goalMap = responses.stream()
                .collect(Collectors.toMap(GoalResponse::getUuid, goal -> goal));
        
        // Build the tree structure
        List<GoalResponse> rootGoals = responses.stream()
                .filter(goal -> goal.getParentGoalId() == null)
                .collect(Collectors.toList());
        
        // Assign children to their parents
        responses.stream()
                .filter(goal -> goal.getParentGoalId() != null)
                .forEach(goal -> {
                    GoalResponse parent = goalMap.get(goal.getParentGoalId());
                    if (parent != null) {
                        if (parent.getChildGoals() == null) {
                            parent.setChildGoals(new ArrayList<>());
                        }
                        parent.getChildGoals().add(goal);
                    }
                });
        
        return rootGoals;
    }
    
    /**
     * Calculate progress percentage based on current and target values
     */
    public Double calculateProgressPercentage(Goal goal) {
        if (goal.getTargetValue() == null || goal.getTargetValue() == 0) {
            return 0.0;
        }
        
        Double currentValue = goal.getCurrentValue() != null ? goal.getCurrentValue() : 0.0;
        Double targetValue = goal.getTargetValue();
        
        switch (goal.getTargetOperator()) {
            case GREATER_THAN:
                return Math.min(100.0, (currentValue / targetValue) * 100.0);
            case EQUAL:
                return currentValue.equals(targetValue) ? 100.0 : 0.0;
            case LESS_THAN:
                if (currentValue <= targetValue) {
                    return 100.0;
                } else {
                    // Progress decreases as we exceed the target
                    return Math.max(0.0, 100.0 - ((currentValue - targetValue) / targetValue) * 100.0);
                }
            default:
                return 0.0;
        }
    }
    
    /**
     * Auto-update status based on progress and target date
     */
    public void updateStatusBasedOnProgress(Goal goal) {
        LocalDateTime now = LocalDateTime.now();
        
        // If progress is 100%, mark as completed
        if (goal.getProgressPercentage() >= 100.0 && goal.getStatus() != Goal.Status.COMPLETED) {
            goal.setStatus(Goal.Status.COMPLETED);
            goal.setCompletedDate(now);
        }
        
        // If target date has passed and not completed, mark as overdue
        if (goal.getTargetDate() != null && 
            goal.getTargetDate().isBefore(now) && 
            goal.getStatus() != Goal.Status.COMPLETED) {
            goal.setStatus(Goal.Status.OVERDUE);
        }
        
        // If progress > 0 and < 100 and not overdue, mark as in progress
        if (goal.getProgressPercentage() > 0.0 && 
            goal.getProgressPercentage() < 100.0 && 
            goal.getStatus() != Goal.Status.OVERDUE &&
            goal.getStatus() == Goal.Status.NOT_STARTED) {
            goal.setStatus(Goal.Status.IN_PROGRESS);
        }
    }
}
