package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.models.GoalWeeklySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalWeeklySnapshotRepository 
    extends JpaRepository<GoalWeeklySnapshot, Long> {

    // Find snapshot for a specific goal and week
    // Used to get or create snapshot when activity is logged
    Optional<GoalWeeklySnapshot> findByGoalIdAndWeekStart(
        Long goalId, 
        LocalDate weekStart
    );

    // Find all snapshots for a goal between two dates
    // Used by momentum calculation (last 4 weeks)
    List<GoalWeeklySnapshot> findByGoalIdAndWeekStartBetween(
        Long goalId,
        LocalDate startDate,
        LocalDate endDate
    );

    // Find all snapshots for a goal ordered by week desc
    // Used for streak calculation and history display
    List<GoalWeeklySnapshot> findByGoalIdOrderByWeekStartDesc(
        Long goalId
    );

    // Find all snapshots for a user in a date range
    // Used by weekly reset job
    List<GoalWeeklySnapshot> findByUserIdAndWeekStart(
        String userId,
        LocalDate weekStart
    );

    // Find latest snapshot for a goal (for healthScoreLastWeek)
    // Returns the most recent snapshot BEFORE current week
    List<GoalWeeklySnapshot> findByGoalIdAndWeekStartBeforeOrderByWeekStartDesc(
        Long goalId,
        LocalDate weekStart
    );

    // Check if snapshot exists for goal and week
    boolean existsByGoalIdAndWeekStart(
        Long goalId,
        LocalDate weekStart
    );

    // Find all snapshots for a specific week across all goals
    // Used by weekly reset job to find goals needing new snapshots
    List<GoalWeeklySnapshot> findByWeekStart(LocalDate weekStart);
}
