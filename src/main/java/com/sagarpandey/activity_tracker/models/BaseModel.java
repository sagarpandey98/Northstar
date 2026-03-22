package com.sagarpandey.activity_tracker.models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public class BaseModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Use this as the unique identifier for the frontend
    private UUID uuid = UUID.randomUUID(); // Internal UUID
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private boolean isDeleted;
    private String userId;
}