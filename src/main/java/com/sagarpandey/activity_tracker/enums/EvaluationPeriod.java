package com.sagarpandey.activity_tracker.enums;

public enum EvaluationPeriod {
    DAILY,      // target per day
    WEEKLY,     // target per week (aligns with existing system)
    MONTHLY,    // target per month
    QUARTERLY,  // target per 3 months
    YEARLY,     // target per year
    CUSTOM      // target per N days (defined by customPeriodDays)
}
