package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.models.GoalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalPeriodRepository extends JpaRepository<GoalPeriod, Long> {

    // Fetch all periods tied to a specific Master Goal
    List<GoalPeriod> findByParentGoalUuid(String parentGoalUuid);

    // Fetch a specific period by its strict UUID
    Optional<GoalPeriod> findByUuid(String uuid);

    @org.springframework.data.jpa.repository.Query("SELECT gp FROM GoalPeriod gp WHERE gp.parentGoalUuid = :parentGoalUuid AND gp.periodStart <= :date AND gp.periodEnd >= :date")
    Optional<GoalPeriod> findActivePeriodForGoal(String parentGoalUuid, java.time.LocalDate date);

}
