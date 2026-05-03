package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.dtos.health.GoalPeriodExpectation;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import java.time.LocalDate;

public interface GoalPeriodExpectationService {

    GoalPeriodExpectation buildExpectation(Goal goal, GoalPeriod period);

    GoalPeriodExpectation buildExpectation(Goal goal, GoalPeriod period, LocalDate evaluationDate);
}
