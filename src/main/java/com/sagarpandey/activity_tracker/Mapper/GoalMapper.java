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
    
    private String normalizeParentGoalId(String parentGoalId) {
        if (parentGoalId == null) {
            return null;
        }

        String trimmed = parentGoalId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Goal toEntity(GoalRequest request, String userId) {
        Goal goal = new Goal();
        goal.setUuid(UUID.randomUUID().toString());
        goal.setUserId(userId);
        boolean milestone = Boolean.TRUE.equals(request.getIsMilestone());
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setPriority(request.getPriority());
        goal.setStatus(request.getStatus() != null ? request.getStatus() : Goal.Status.NOT_STARTED);
        goal.setMetric(request.getMetric());
        goal.setTargetOperator(request.getTargetOperator());
        goal.setTargetValue(request.getTargetValue());
        goal.setCurrentValue(request.getCurrentValue() != null ? request.getCurrentValue() : 0.0);
        if (milestone) {
            goal.setStartDate(request.getStartDate());
        } else {
            goal.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
        }
        goal.setTargetDate(request.getTargetDate());
        goal.setParentGoalId(request.getParentGoalId());
        goal.setIsMilestone(request.getIsMilestone() != null ? request.getIsMilestone() : false);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setLastUpdatedAt(LocalDateTime.now());
        goal.setIsDeleted(false);
        applyMilestoneTrackingDefaults(goal);

        // New Ledger Fields
        goal.setGoalType(request.getGoalType());
        goal.setScheduleSpec(request.getScheduleSpec());
        goal.setMinimumSessionPeriod(request.getMinimumSessionPeriod());
        goal.setMaximumSessionPeriod(request.getMaximumSessionPeriod());
        goal.setMinimumTimeCommittedPeriod(request.getMinimumTimeCommittedPeriod());
        goal.setMinimumTimeCommittedPerActivity(request.getMinimumTimeCommittedPerActivity());
        goal.setAllowDoubleLogging(request.getAllowDoubleLogging() != null ? request.getAllowDoubleLogging() : Boolean.TRUE);
        goal.setMissesAllowedPerPeriod(request.getMissesAllowedPerPeriod());

        GoalWeightValidator.validateWeights(request.getConsistencyWeight(), request.getMomentumWeight(), request.getProgressWeight());

        // Set weights - use provided values or goal type defaults
        if (request.getConsistencyWeight() != null) {
            goal.setConsistencyWeight(request.getConsistencyWeight());
            goal.setMomentumWeight(request.getMomentumWeight());
            goal.setProgressWeight(request.getProgressWeight());
        } else {
            // Use goal type defaults when weights not provided in request
            goal.setConsistencyWeight(goal.getEffectiveConsistencyWeight());
            goal.setMomentumWeight(goal.getEffectiveMomentumWeight());
            goal.setProgressWeight(goal.getEffectiveProgressWeight());
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
        if (request.getIsMilestone() != null) {
            goal.setIsMilestone(request.getIsMilestone());
        }
        boolean milestone = Boolean.TRUE.equals(goal.getIsMilestone());
        if (milestone) {
            if (request.getMetric() != null) {
                goal.setMetric(request.getMetric());
            }
            if (request.getTargetOperator() != null) {
                goal.setTargetOperator(request.getTargetOperator());
            }
            if (request.getTargetValue() != null) {
                goal.setTargetValue(request.getTargetValue());
            }
        } else {
            goal.setMetric(request.getMetric());
            goal.setTargetOperator(request.getTargetOperator());
            goal.setTargetValue(request.getTargetValue());
        }
        if (request.getCurrentValue() != null) {
            goal.setCurrentValue(request.getCurrentValue());
        }
        if (request.getStartDate() != null) {
            goal.setStartDate(request.getStartDate());
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }
        goal.setParentGoalId(request.getParentGoalId());
        goal.setLastUpdatedAt(LocalDateTime.now());

        // New Ledger Fields
        if (request.getGoalType() != null) {
            goal.setGoalType(request.getGoalType());
        }
        goal.setScheduleSpec(request.getScheduleSpec());
        goal.setMinimumSessionPeriod(request.getMinimumSessionPeriod());
        goal.setMaximumSessionPeriod(request.getMaximumSessionPeriod());
        goal.setMinimumTimeCommittedPeriod(request.getMinimumTimeCommittedPeriod());
        goal.setMinimumTimeCommittedPerActivity(request.getMinimumTimeCommittedPerActivity());
        goal.setAllowDoubleLogging(request.getAllowDoubleLogging() != null ? request.getAllowDoubleLogging() : Boolean.TRUE);
        goal.setMissesAllowedPerPeriod(request.getMissesAllowedPerPeriod());

        GoalWeightValidator.validateWeights(request.getConsistencyWeight(), request.getMomentumWeight(), request.getProgressWeight());

        // Set weights - use provided values or keep existing values
        if (request.getConsistencyWeight() != null) {
            goal.setConsistencyWeight(request.getConsistencyWeight());
            goal.setMomentumWeight(request.getMomentumWeight());
            goal.setProgressWeight(request.getProgressWeight());
        }
        // If no weights provided, they'll use goal type defaults via getEffective*Weight() methods
        
        goal.setProgressPercentage(calculateProgressPercentage(goal));
        applyMilestoneTrackingDefaults(goal);
        updateStatusBasedOnProgress(goal);
    }

    /**
     * Milestone goals are not user-tracked; DB still requires metric / operator / targetValue.
     */
    private void applyMilestoneTrackingDefaults(Goal goal) {
        if (!Boolean.TRUE.equals(goal.getIsMilestone())) {
            return;
        }
        if (goal.getMetric() == null) {
            goal.setMetric(Goal.Metric.COUNT);
        }
        if (goal.getTargetOperator() == null) {
            goal.setTargetOperator(Goal.TargetOperator.EQUAL);
        }
        if (goal.getTargetValue() == null) {
            goal.setTargetValue(0.0);
        }
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
        response.setParentGoalId(normalizeParentGoalId(goal.getParentGoalId()));
        response.setIsMilestone(goal.getIsMilestone());
        response.setCreatedAt(goal.getCreatedAt());
        response.setLastUpdatedAt(goal.getLastUpdatedAt());

        // Ledger specific
        response.setGoalType(goal.getGoalType());
        response.setIsLeaf(rollupService.isLeafGoal(goal.getUuid(), goal.getUserId()));
        response.setIsTracked(goal.getScheduleSpec() != null);
        response.setScheduleSpec(goal.getScheduleSpec());
        response.setMinimumSessionPeriod(goal.getMinimumSessionPeriod());
        response.setMaximumSessionPeriod(goal.getMaximumSessionPeriod());
        response.setMinimumTimeCommittedPeriod(goal.getMinimumTimeCommittedPeriod());
        response.setMinimumTimeCommittedPerActivity(goal.getMinimumTimeCommittedPerActivity());
        response.setAllowDoubleLogging(goal.getAllowDoubleLogging());
        response.setMissesAllowedPerPeriod(goal.getMissesAllowedPerPeriod());
        response.setConsistencyWeight(goal.getConsistencyWeight() != null ? 
            goal.getConsistencyWeight() : goal.getEffectiveConsistencyWeight());
        response.setMomentumWeight(goal.getMomentumWeight() != null ? 
            goal.getMomentumWeight() : goal.getEffectiveMomentumWeight());
        response.setProgressWeight(goal.getProgressWeight() != null ? 
            goal.getProgressWeight() : goal.getEffectiveProgressWeight());
        response.setConsistencyScore(goal.getConsistencyScore());
        response.setMomentumScore(goal.getMomentumScore());
        response.setProgressScore(goal.getProgressScore());
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
        List<GoalResponse> rootGoals = responses.stream()
                .filter(goal -> normalizeParentGoalId(goal.getParentGoalId()) == null)
                .collect(Collectors.toList());
        
        responses.stream()
                .filter(goal -> normalizeParentGoalId(goal.getParentGoalId()) != null)
                .forEach(goal -> {
                    GoalResponse parent = goalMap.get(normalizeParentGoalId(goal.getParentGoalId()));
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
