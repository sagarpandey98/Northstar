package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.models.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import java.time.OffsetDateTime;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    List<Activity> findAllByUserId(String userId);

    List<Activity> findByStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndUserId(OffsetDateTime startTime, OffsetDateTime endTime, String userId);
}