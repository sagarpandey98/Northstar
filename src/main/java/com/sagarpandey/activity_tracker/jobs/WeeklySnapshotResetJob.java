package com.sagarpandey.activity_tracker.jobs;

import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Repository.GoalWeeklySnapshotRepository;
import com.sagarpandey.activity_tracker.Service.Inteface.GoalHealthService;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalWeeklySnapshot;
import com.sagarpandey.activity_tracker.utils.WeekUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class WeeklySnapshotResetJob {

    private static final Logger log =
        LoggerFactory.getLogger(WeeklySnapshotResetJob.class);

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalWeeklySnapshotRepository snapshotRepository;

    @Autowired
    private GoalHealthService goalHealthService;

    /**
     * Runs every Monday at 00:05 AM.
     * 1. Creates empty snapshots for new week for all
     *    active goals (so momentum has 0-entries for
     *    weeks with no activity)
     * 2. Recalculates health for all active goals so
     *    scores reflect the new week's starting state
     */
    @Scheduled(cron = "0 5 0 * * MON")
    @Transactional
    public void runWeeklyReset() {
        log.info("Weekly snapshot reset job started");

        LocalDate newWeekMonday = WeekUtils.getCurrentWeekMonday();

        // Fetch all active goals across all users
        // "active" = not deleted and not COMPLETED
        List<Goal> activeGoals = goalRepository
            .findAllActiveGoals();

        int created = 0;
        int skipped = 0;
        int healthUpdated = 0;

        for (Goal goal : activeGoals) {
            try {
                // Only create snapshot if targetFrequency is set
                if (goal.getTargetFrequencyWeekly() != null
                        && goal.getTargetFrequencyWeekly() > 0) {

                    boolean exists = snapshotRepository
                        .existsByGoalIdAndWeekStart(
                            goal.getId(), newWeekMonday
                        );

                    if (!exists) {
                        GoalWeeklySnapshot snapshot =
                            new GoalWeeklySnapshot();
                        snapshot.setGoalId(goal.getId());
                        snapshot.setUserId(goal.getUserId());
                        snapshot.setWeekStart(newWeekMonday);
                        snapshot.setActivitiesLogged(0);
                        snapshot.setTargetFrequencyWeekly(
                            goal.getTargetFrequencyWeekly()
                        );
                        snapshot.setConsistencyScoreForWeek(0.0);
                        snapshotRepository.save(snapshot);
                        created++;
                    } else {
                        skipped++;
                    }
                }

                // Recalculate health for all active goals
                goalHealthService.recalculateHealth(goal.getId());
                healthUpdated++;

            } catch (Exception e) {
                log.error("Weekly reset failed for goalId={}: {}",
                    goal.getId(), e.getMessage());
            }
        }

        log.info("Weekly reset complete: created={}, " +
            "skipped={}, healthUpdated={}",
            created, skipped, healthUpdated);
    }
}
