package com.sagarpandey.activity_tracker.Mapper;

import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Interface.RollupService;
import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.validators.GoalWeightValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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
    
    public Goal toEntity(GoalRequest request, String userId) {
        Goal goal = new Goal();
        goal.setUuid(UUID.randomUUID().toString());
        goal.setUserId(userId);
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
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

        // New Ledger Fields
        goal.setGoalType(request.getGoalType());
        goal.setScheduleType(request.getScheduleType() != null ? request.getScheduleType() : goal.getDefaultScheduleType());
        goal.setScheduleSpec(request.getScheduleSpec());
        goal.setMinimumSessionPeriod(request.getMinimumSessionPeriod());
        goal.setMaximumSessionPeriod(request.getMaximumSessionPeriod());
        goal.setMinimumTimeCommittedPeriod(request.getMinimumTimeCommittedPeriod());
        goal.setMinimumTimeCommittedDaily(request.getMinimumTimeCommittedDaily());
        goal.setAllowDoubleLogging(request.getAllowDoubleLogging() != null ? request.getAllowDoubleLogging() : Boolean.TRUE);
        goal.setMissesAllowedPerPeriod(request.getMissesAllowedPerPeriod());

        GoalWeightValidator.validateWeights(request.getConsistencyWeight(), request.getMomentumWeight(), request.getProgressWeight());

        if (request.getConsistencyWeight() != null) {
            goal.setConsistencyWeight(request.getConsistencyWeight());
            goal.setMomentumWeight(request.getMomentumWeight());
            goal.setProgressWeight(request.getProgressWeight());
        }
        
        goal.setProgressPercentage(calculateProgressPercentage(goal));
        return goal;
    }
    
    public void updateEntity(Goal goal, GoalRequest request) {
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
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

        // New Ledger Fields
        goal.setGoalType(request.getGoalType());
        goal.setScheduleType(request.getScheduleType() != null ? request.getScheduleType() : goal.getDefaultScheduleType());
        goal.setScheduleSpec(request.getScheduleSpec());
        goal.setMinimumSessionPeriod(request.getMinimumSessionPeriod());
        goal.setMaximumSessionPeriod(request.getMaximumSessionPeriod());
        goal.setMinimumTimeCommittedPeriod(request.getMinimumTimeCommittedPeriod());
        goal.setMinimumTimeCommittedDaily(request.getMinimumTimeCommittedDaily());
        goal.setAllowDoubleLogging(request.getAllowDoubleLogging() != null ? request.getAllowDoubleLogging() : Boolean.TRUE);
        goal.setMissesAllowedPerPeriod(request.getMissesAllowedPerPeriod());

        GoalWeightValidator.validateWeights(request.getConsistencyWeight(), request.getMomentumWeight(), request.getProgressWeight());

        if (request.getConsistencyWeight() != null) {
            goal.setConsistencyWeight(request.getConsistencyWeight());
            goal.setMomentumWeight(request.getMomentumWeight());
            goal.setProgressWeight(request.getProgressWeight());
        }
        
        goal.setProgressPercentage(calculateProgressPercentage(goal));
        updateStatusBasedOnProgress(goal);
    }
    
    public GoalResponse toResponse(Goal goal) {
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setUuid(goal.getUuid());
        response.setUserId(goal.getUserId());
        response.setTitle(goal.getTitle());
        response.setDescription(goal.getDescription());
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

        // Ledger specific
        response.setGoalType(goal.getGoalType());
        response.setIsLeaf(rollupService.isLeafGoal(goal.getUuid(), goal.getUserId()));
        response.setIsTracked(goal.getScheduleSpec() != null);
        response.setScheduleType(goal.getScheduleType());
        response.setScheduleSpec(goal.getScheduleSpec());
        response.setMinimumSessionPeriod(goal.getMinimumSessionPeriod());
        response.setMaximumSessionPeriod(goal.getMaximumSessionPeriod());
        response.setMinimumTimeCommittedPeriod(goal.getMinimumTimeCommittedPeriod());
        response.setMinimumTimeCommittedDaily(goal.getMinimumTimeCommittedDaily());
        response.setAllowDoubleLogging(goal.getAllowDoubleLogging());
        response.setMissesAllowedPerPeriod(goal.getMissesAllowedPerPeriod());
        response.setScheduleDays(goal.getScheduleDays());
        response.setScheduleDaysList(goal.getScheduleDaysList());
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

        if (!response.getIsLeaf()) {
            List<Goal> children = goalRepository.findByParentGoalIdAndUserIdAndIsDeletedFalse(goal.getUuid(), goal.getUserId());
            Double rolledUpScore = rollupService.calculateRolledUpHealthScore(children);
            if (rolledUpScore != null) {
                response.setHealthScore(rolledUpScore);
            }
        } else {
            response.setHealthScore(goal.getHealthScore());
        }

        response.setCurrentStreak(goal.getCurrentStreak());
        response.setLongestStreak(goal.getLongestStreak());

        if (!response.getIsLeaf()) {
            response.setParentInsights(rollupService.buildParentInsights(goal.getId(), goal.getUserId()));
        } else {
            response.setParentInsights(null);
        }

        return response;
    }
    
    public List<GoalResponse> toResponseList(List<Goal> goals) {
        return goals.stream().map(this::toResponse).collect(Collectors.toList());
    }
    
    public List<GoalResponse> buildGoalTree(List<Goal> goals) {
        List<GoalResponse> responses = toResponseList(goals);
        Map<String, GoalResponse> goalMap = responses.stream().collect(Collectors.toMap(GoalResponse::getUuid, goal -> goal));
        List<GoalResponse> rootGoals = responses.stream().filter(goal -> goal.getParentGoalId() == null).collect(Collectors.toList());
        
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
    
    public Double calculateProgressPercentage(Goal goal) {
        if (goal.getTargetValue() == null || goal.getTargetValue() == 0) return 0.0;
        Double currentValue = goal.getCurrentValue() != null ? goal.getCurrentValue() : 0.0;
        Double targetValue = goal.getTargetValue();
        
        switch (goal.getTargetOperator()) {
            case GREATER_THAN: return Math.min(100.0, (currentValue / targetValue) * 100.0);
            case EQUAL: return currentValue.equals(targetValue) ? 100.0 : 0.0;
            case LESS_THAN:
                if (currentValue <= targetValue) return 100.0;
                else return Math.max(0.0, 100.0 - ((currentValue - targetValue) / targetValue) * 100.0);
            default: return 0.0;
        }
    }
    
    public void updateStatusBasedOnProgress(Goal goal) {
        LocalDateTime now = LocalDateTime.now();
        if (goal.getProgressPercentage() >= 100.0 && goal.getStatus() != Goal.Status.COMPLETED) {
            goal.setStatus(Goal.Status.COMPLETED);
            goal.setCompletedDate(now);
        }
        if (goal.getTargetDate() != null && goal.getTargetDate().isBefore(now) && goal.getStatus() != Goal.Status.COMPLETED) {
            goal.setStatus(Goal.Status.OVERDUE);
        }
        if (goal.getProgressPercentage() > 0.0 && goal.getProgressPercentage() < 100.0 && 
            goal.getStatus() != Goal.Status.OVERDUE && goal.getStatus() == Goal.Status.NOT_STARTED) {
            goal.setStatus(Goal.Status.IN_PROGRESS);
        }
    }
}
