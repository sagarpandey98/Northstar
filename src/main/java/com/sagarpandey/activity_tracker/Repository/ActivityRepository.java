package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.models.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.OffsetDateTime;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    List<Activity> findAllByUserId(String userId);

    List<Activity> findByStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndUserId(OffsetDateTime startTime, OffsetDateTime endTime, String userId);

    @Query("""
        SELECT a FROM Activity a
        WHERE a.goalId = :goalId
          AND a.userId = :userId
          AND a.startTime < :periodEndExclusive
          AND COALESCE(a.endTime, a.startTime) >= :periodStartInclusive
    """)
    List<Activity> findGoalActivitiesOverlappingPeriod(
        @Param("goalId") Long goalId,
        @Param("userId") String userId,
        @Param("periodStartInclusive") OffsetDateTime periodStartInclusive,
        @Param("periodEndExclusive") OffsetDateTime periodEndExclusive
    );
}
