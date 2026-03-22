package com.sagarpandey.activity_tracker.enums;

public enum HealthStatus {
    THRIVING,    // healthScore >= 80
    ON_TRACK,    // healthScore >= 60
    AT_RISK,     // healthScore >= 40
    CRITICAL,    // healthScore < 40
    UNTRACKED    // healthScore is null
}
