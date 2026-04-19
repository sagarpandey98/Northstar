package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.Goal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GoalPeriodService {
    
    // ============================================
    // CORE PERIOD MANAGEMENT
    // ============================================
    
    /**
     * Create a new GoalPeriod for a Goal with intelligent field copying
     * and period range calculation based on goal's schedule frequency
     */
    GoalPeriod createPeriodForGoal(Goal goal);
    
    /**
     * Create a new GoalPeriod with custom period range
     */
    GoalPeriod createPeriodForGoal(Goal goal, LocalDate periodStart, LocalDate periodEnd);
    
    /**
     * Get all periods for a goal sorted by period start date
     */
    List<GoalPeriod> getPeriodsForGoal(String goalUuid);
    
    /**
     * Get active period for a goal on a specific date
     */
    Optional<GoalPeriod> getActivePeriodForGoal(String goalUuid, LocalDate date);
    
    /**
     * Update an existing period
     */
    GoalPeriod updatePeriod(GoalPeriod period);
    
    /**
     * Delete a period by UUID
     */
    void deletePeriod(String periodUuid);
    
    /**
     * Create next period in sequence for a goal
     */
    Optional<GoalPeriod> createNextPeriod(String goalUuid);
    
    // ============================================
    // PERIOD RANGE CALCULATION
    // ============================================
    
    /**
     * Calculate period start and end dates based on goal's schedule frequency
     * and reference date (for subsequent periods)
     */
    PeriodRange calculatePeriodRange(Goal goal, LocalDate referenceDate);
    
    /**
     * Calculate first period range for a new goal
     */
    PeriodRange calculateFirstPeriodRange(Goal goal);
    
    // ============================================
    // UTILITY CLASSES
    // ============================================
    
    /**
     * Represents a calculated period range
     */
    class PeriodRange {
        private final LocalDate periodStart;
        private final LocalDate periodEnd;
        
        public PeriodRange(LocalDate periodStart, LocalDate periodEnd) {
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
        }
        
        public LocalDate getPeriodStart() { return periodStart; }
        public LocalDate getPeriodEnd() { return periodEnd; }
        
        @Override
        public String toString() {
            return "PeriodRange{start=" + periodStart + ", end=" + periodEnd + "}";
        }
    }
}
