package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.models.GoalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface GoalPeriodRepository extends JpaRepository<GoalPeriod, Long> {

    // Fetch all periods tied to a specific Master Goal
    List<GoalPeriod> findByParentGoalUuid(String parentGoalUuid);

    List<GoalPeriod> findByParentGoalUuidIn(List<String> parentGoalUuids);

    Optional<GoalPeriod> findByParentGoalUuidAndUuid(String parentGoalUuid, String uuid);

    Optional<GoalPeriod> findTopByParentGoalUuidOrderByPeriodEndDesc(String parentGoalUuid);

    boolean existsByParentGoalUuidAndPeriodStartAndPeriodEnd(String parentGoalUuid, LocalDate periodStart, LocalDate periodEnd);

    // Fetch a specific period by its strict UUID
    Optional<GoalPeriod> findByUuid(String uuid);

    @org.springframework.data.jpa.repository.Query("SELECT gp FROM GoalPeriod gp WHERE gp.parentGoalUuid = :parentGoalUuid AND gp.periodStart <= :date AND gp.periodEnd >= :date")
    Optional<GoalPeriod> findActivePeriodForGoal(String parentGoalUuid, java.time.LocalDate date);

}
