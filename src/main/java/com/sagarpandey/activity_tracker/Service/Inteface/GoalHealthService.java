package com.sagarpandey.activity_tracker.Service.Inteface;

public interface GoalHealthService {

    /**
     * Called when an activity linked to a goal is saved.
     * Updates the weekly snapshot and recalculates all
     * health scores for the goal.
     *
     * @param goalId       the goal to update
     * @param activityDate the date of the logged activity
     */
    void onActivityLogged(Long goalId, java.time.LocalDate activityDate);

    /**
     * Recalculates health scores for a specific goal.
     * Can be called manually or by scheduled job.
     *
     * @param goalId the goal to recalculate
     */
    void recalculateHealth(Long goalId);

    /**
     * Recalculates health scores for all active goals
     * of a user. Called by weekly scheduled job.
     *
     * @param userId the user whose goals to recalculate
     */
    void recalculateAllHealthForUser(String userId);
}
