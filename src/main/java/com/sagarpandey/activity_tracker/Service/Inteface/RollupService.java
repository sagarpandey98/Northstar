package com.sagarpandey.activity_tracker.Service.Inteface;

import com.sagarpandey.activity_tracker.dtos.ParentInsights;
import com.sagarpandey.activity_tracker.models.Goal;
import java.util.List;

public interface RollupService {

    /**
     * Calculates rolled-up health score for a parent goal
     * based on its children's health scores.
     * Uses priority-weighted average.
     *
     * @param children list of direct child Goal entities
     * @return weighted health score 0-100, or null if no
     *         children have health scores
     */
    Double calculateRolledUpHealthScore(List<Goal> children);

    /**
     * Builds the ParentInsights block for a non-leaf goal.
     * Only call this for goals that have children.
     *
     * @param goalId  the parent goal id
     * @param userId  the user id
     * @return populated ParentInsights or null if no children
     */
    ParentInsights buildParentInsights(Long goalId, String userId);

    /**
     * Determines if a goal is a leaf node
     * (has no active children).
     *
     * @param goalUuid the goal uuid
     * @param userId   the user id
     * @return true if leaf, false if parent
     */
    boolean isLeafGoal(String goalUuid, String userId);
}
