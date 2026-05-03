package com.sagarpandey.activity_tracker.Mapper;

import com.sagarpandey.activity_tracker.dtos.GoalPeriodResponse;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GoalPeriodMapper {

    public GoalPeriodResponse toResponse(GoalPeriod period) {
        GoalPeriodResponse response = new GoalPeriodResponse();
        response.setId(period.getId());
        response.setUuid(period.getUuid());
        response.setGoalId(period.getParentGoalUuid());
        response.setPeriodStart(period.getPeriodStart());
        response.setPeriodEnd(period.getPeriodEnd());
        response.setCurrentValue(period.getCurrentValue());
        response.setProgressPercentage(period.getProgressPercentage());
        response.setHealthStatus(period.getHealthStatus());
        response.setHealthScore(period.getHealthScore());
        response.setConsistencyScore(period.getConsistencyScore());
        response.setMomentumScore(period.getMomentumScore());
        response.setProgressScore(period.getProgressScore());
        response.setCurrentStreak(period.getCurrentStreak());
        response.setLongestStreak(period.getLongestStreak());
        response.setMinimumSessionDaily(period.getMinimumSessionDaily());
        response.setScheduleSpec(period.getScheduleSpec());
        response.setCreatedAt(period.getCreatedAt());
        response.setLastUpdatedAt(period.getLastUpdatedAt());
        return response;
    }

    public List<GoalPeriodResponse> toResponseList(List<GoalPeriod> periods) {
        return periods.stream().map(this::toResponse).toList();
    }
}
