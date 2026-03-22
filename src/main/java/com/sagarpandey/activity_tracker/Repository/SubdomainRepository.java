package com.sagarpandey.activity_tracker.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sagarpandey.activity_tracker.models.Subdomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubdomainRepository extends JpaRepository<Subdomain, Long> {
    List<Subdomain> findAllByUserId(String userId);
    Optional<Subdomain> findByUuid(UUID uuid);
    Optional<Subdomain> findByUuidAndUserId(UUID uuid, String userId);
}