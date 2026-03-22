package com.sagarpandey.activity_tracker.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sagarpandey.activity_tracker.models.Domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainRepository extends JpaRepository<Domain, Long> {
    List<Domain> findAllByUserId(String userId);
    Optional<Domain> findByUuid(UUID uuid);
    Optional<Domain> findByUuidAndUserId(UUID uuid, String userId);
}