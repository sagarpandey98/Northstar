package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.enums.EvaluationPeriod;
import com.sagarpandey.activity_tracker.models.GoalPeriodSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalPeriodSnapshotRepository
    extends JpaRepository<GoalPeriodSnapshot, Long> {

    // Find snapshot for specific goal, period type and start
    Optional<GoalPeriodSnapshot> findByGoalIdAndPeriodTypeAndPeriodStart(
        Long goalId,
        EvaluationPeriod periodType,
        LocalDate periodStart
    );

    // Find all snapshots for a goal of a specific period type
    // ordered by period start desc (most recent first)
    List<GoalPeriodSnapshot> findByGoalIdAndPeriodTypeOrderByPeriodStartDesc(
        Long goalId,
        EvaluationPeriod periodType
    );

    // Find snapshots for momentum calculation
    // (last N periods of a specific type)
    List<GoalPeriodSnapshot> findByGoalIdAndPeriodTypeAndPeriodStartBetween(
        Long goalId,
        EvaluationPeriod periodType,
        LocalDate startDate,
        LocalDate endDate
    );

    // Check if snapshot exists
    boolean existsByGoalIdAndPeriodTypeAndPeriodStart(
        Long goalId,
        EvaluationPeriod periodType,
        LocalDate periodStart
    );

    // Find all snapshots for a user of a specific period type
    // starting on a specific date
    // Used by period reset job
    List<GoalPeriodSnapshot> findByUserIdAndPeriodTypeAndPeriodStart(
        String userId,
        EvaluationPeriod periodType,
        LocalDate periodStart
    );
}
