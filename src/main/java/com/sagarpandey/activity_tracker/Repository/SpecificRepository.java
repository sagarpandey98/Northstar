package com.sagarpandey.activity_tracker.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sagarpandey.activity_tracker.models.Specifics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecificRepository extends JpaRepository<Specifics, Long> {
    List<Specifics> findAllByUserId(String userId);
    Optional<Specifics> findByUuid(UUID uuid);
    Optional<Specifics> findByUuidAndUserId(UUID uuid, String userId);
}