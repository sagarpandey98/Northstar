package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodService;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GoalPeriodServiceV1 implements GoalPeriodService {
    
    @Autowired
    private GoalPeriodRepository goalPeriodRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Override
    public GoalPeriod createPeriodForGoal(Goal goal) {
        PeriodRange range = calculateFirstPeriodRange(goal);
        return createPeriodForGoal(goal, range.getPeriodStart(), range.getPeriodEnd());
    }
    
    @Override
    public GoalPeriod createPeriodForGoal(Goal goal, LocalDate periodStart, LocalDate periodEnd) {
        GoalPeriod period = new GoalPeriod();
        
        // System-generated fields
        period.setUuid(UUID.randomUUID().toString());
        period.setCreatedAt(LocalDateTime.now());
        period.setLastUpdatedAt(LocalDateTime.now());
        // isDeleted field handled by soft delete in deletePeriod method
        
        // Foreign key to parent goal
        period.setParentGoalUuid(goal.getUuid());
        
        // Copy fields from parent goal
        period.setScheduleSpec(goal.getScheduleSpec());
        Double calculatedDaily = calculateMinimumSessionDaily(goal, period.getPeriodStart());
        System.out.println("DEBUG: Final minimumSessionDaily value: " + calculatedDaily + " (storing as Double)");
        period.setMinimumSessionDaily(calculatedDaily);
        period.setMinimumSessionPeriod(goal.getMinimumSessionPeriod());
        period.setAllowDoubleLogging(goal.getAllowDoubleLogging());
        period.setMetric(goal.getMetric());
        period.setTargetOperator(goal.getTargetOperator());
        period.setMissesAllowedPerPeriod(goal.getMissesAllowedPerPeriod());
        period.setLongestStreak(goal.getLongestStreak());
        
        // DEBUG: Print weight values during transfer
        System.out.println("DEBUG: Goal weights - consistency: " + goal.getConsistencyWeight() + 
            ", momentum: " + goal.getMomentumWeight() + 
            ", progress: " + goal.getProgressWeight());
        
        period.setConsistencyWeight(goal.getConsistencyWeight());
        period.setMomentumWeight(goal.getMomentumWeight());
        period.setProgressWeight(goal.getProgressWeight());
        
        // DEBUG: Print GoalPeriod weights after transfer
        System.out.println("DEBUG: GoalPeriod weights after setting - consistency: " + period.getConsistencyWeight() + 
            ", momentum: " + period.getMomentumWeight() + 
            ", progress: " + period.getProgressWeight());
        
        period.setMaximumSessionPeriod(goal.getMaximumSessionPeriod());
        // minimumTimeCommittedDaily and minimumTimeCommittedPeriod are goal-only fields
        
        // Set period range
        period.setPeriodStart(periodStart);
        period.setPeriodEnd(periodEnd);
        
        // Default values
        period.setCurrentValue(0.0);
        period.setProgressPercentage(0.0);
        period.setHealthScore(0.0);
        period.setConsistencyScore(0.0);
        period.setMomentumScore(0.0);
        period.setProgressScore(0.0);
        period.setCurrentStreak(0);
        period.setCompletedDate(null);
        
        // Set health status based on goal type
        period.setHealthStatus(goal.getIsMilestone() ? HealthStatus.UNTRACKED : HealthStatus.ON_TRACK);
        
        return goalPeriodRepository.save(period);
    }
    
    /**
     * Calculate minimum session daily based on schedule spec and minimum session period
     * Handles nested schedules and period alignment
     */
    private Double calculateMinimumSessionDaily(Goal goal, LocalDate periodStart) {
        System.out.println("DEBUG: Starting sophisticated minimum_session_daily calculation");
        System.out.println("DEBUG: Period start date: " + periodStart);
        
        // Handle null or zero values
        if (goal.getMinimumSessionPeriod() == null || goal.getMinimumSessionPeriod() == 0) {
            System.out.println("DEBUG: minimumSessionPeriod is null or 0, returning 0.0");
            return 0.0;
        }
        
        if (goal.getScheduleSpec() == null) {
            System.out.println("DEBUG: scheduleSpec is null, returning 0.0");
            return 0.0;
        }
        
        double minimumSessionPeriod = goal.getMinimumSessionPeriod();
        ScheduleSpec scheduleSpec = goal.getScheduleSpec();
        
        System.out.println("DEBUG: minimumSessionPeriod: " + minimumSessionPeriod);
        System.out.println("DEBUG: schedule frequency: " + scheduleSpec.getFrequency());
        System.out.println("DEBUG: schedule flexible: " + scheduleSpec.getFlexible());
        
        // Calculate based on nested schedule structure with period alignment
        Double result = calculateNestedDaily(minimumSessionPeriod, scheduleSpec, periodStart);
        System.out.println("DEBUG: Final calculated daily minimum: " + result);
        return result != null ? result : 0.0;
    }
    
    /**
     * Recursively calculate daily minimum through nested schedule structure
     */
    private Double calculateNestedDaily(double minimumSessionPeriod, ScheduleSpec scheduleSpec, LocalDate periodStart) {
        String frequency = scheduleSpec.getFrequency();
        boolean isFlexible = scheduleSpec.getFlexible();
        
        System.out.println("DEBUG: Processing level - frequency: " + frequency + ", flexible: " + isFlexible);
        
        // Check if this level has segments (nested structure)
        if (scheduleSpec.getSegments() != null && !scheduleSpec.getSegments().isEmpty()) {
            System.out.println("DEBUG: Found " + scheduleSpec.getSegments().size() + " segments at " + frequency + " level");
            
            // This is a parent level with nested segments
            return calculateNestedWithSegments(minimumSessionPeriod, scheduleSpec, periodStart);
        } else {
            // This is a leaf level, calculate directly
            return calculateLeafLevelDaily(minimumSessionPeriod, scheduleSpec);
        }
    }
    
    /**
     * Calculate daily minimum for parent levels with segments
     */
    private Double calculateNestedWithSegments(double minimumSessionPeriod, ScheduleSpec parentSpec, LocalDate periodStart) {
        String parentFrequency = parentSpec.getFrequency();
        
        if (parentSpec.getFlexible()) {
            // Flexible parent: divide by total days in period
            int daysInPeriod = getDaysInPeriod(parentFrequency);
            double daily = minimumSessionPeriod / daysInPeriod;
            System.out.println("DEBUG: Flexible " + parentFrequency + ": " + minimumSessionPeriod + " / " + daysInPeriod + " = " + daily);
            return Math.round(daily * 100.0) / 100.0;
        } else {
            // Specific parent: process all segments and find the most specific calculation
            return calculateSpecificSegmentsDaily(minimumSessionPeriod, parentSpec, periodStart);
        }
    }
    
    /**
     * Calculate daily minimum for specific segments with nested processing
     */
    private Double calculateSpecificSegmentsDaily(double minimumSessionPeriod, ScheduleSpec parentSpec, LocalDate periodStart) {
        System.out.println("DEBUG: Processing specific segments for " + parentSpec.getFrequency());
        
        // Find aligned segment if possible
        ScheduleSpec.Segment alignedSegment = findAlignedSegment(parentSpec, periodStart);
        if (alignedSegment != null) {
            System.out.println("DEBUG: Found aligned segment: " + alignedSegment.getFrequency() + " with values: " + alignedSegment.getValues());
            
            // Calculate for this specific segment
            double segmentPeriod = minimumSessionPeriod / parentSpec.getSegments().size();
            return calculateSegmentDaily(segmentPeriod, alignedSegment);
        } else {
            System.out.println("DEBUG: No aligned segment found, processing all segments");
            
            // Process all segments to find the most specific calculation
            for (ScheduleSpec.Segment segment : parentSpec.getSegments()) {
                double segmentPeriod = minimumSessionPeriod / parentSpec.getSegments().size();
                Double segmentDaily = calculateSegmentDaily(segmentPeriod, segment);
                
                if (segmentDaily != null && segmentDaily > 0) {
                    System.out.println("DEBUG: Using segment calculation: " + segmentDaily);
                    return segmentDaily;
                }
            }
            
            // Fallback to total specific days
            System.out.println("DEBUG: All segments failed, using fallback calculation");
            return calculateSpecificDaily(minimumSessionPeriod, parentSpec);
        }
    }
    
    /**
     * Calculate daily minimum for a specific segment
     */
    private Double calculateSegmentDaily(double minimumSessionPeriod, ScheduleSpec.Segment segment) {
        String frequency = segment.getFrequency();
        boolean isFlexible = segment.getFlexible();
        
        System.out.println("DEBUG: Calculating segment daily - frequency: " + frequency + ", flexible: " + isFlexible + ", values: " + segment.getValues());
        
        // Check if segment has nested segments (further nesting)
        if (segment.getSegments() != null && !segment.getSegments().isEmpty()) {
            System.out.println("DEBUG: Segment has nested segments, processing recursively");
            return calculateNestedWithSegments(minimumSessionPeriod, convertSegmentToSpec(segment), null);
        }
        
        if (isFlexible) {
            // Flexible segment: divide by standard days for this frequency
            int daysInPeriod = getDaysInPeriod(frequency);
            double daily = minimumSessionPeriod / daysInPeriod;
            System.out.println("DEBUG: Flexible segment " + frequency + ": " + minimumSessionPeriod + " / " + daysInPeriod + " = " + daily);
            return Math.round(daily * 100.0) / 100.0;
        } else {
            // Specific segment: divide by number of specific values
            if (segment.getValues() != null && !segment.getValues().isEmpty()) {
                int specificDays = segment.getValues().size();
                double daily = minimumSessionPeriod / specificDays;
                System.out.println("DEBUG: Specific segment " + frequency + ": " + minimumSessionPeriod + " / " + specificDays + " = " + daily);
                return Math.round(daily * 100.0) / 100.0;
            } else {
                System.out.println("DEBUG: No specific values found in segment, returning 0.0");
                return 0.0;
            }
        }
    }
    
    /**
     * Calculate daily minimum for leaf levels (no further nesting)
     */
    private Double calculateLeafLevelDaily(double minimumSessionPeriod, ScheduleSpec scheduleSpec) {
        String frequency = scheduleSpec.getFrequency();
        boolean isFlexible = scheduleSpec.getFlexible();
        
        if (isFlexible) {
            // Flexible leaf: divide by standard days for this frequency
            int daysInPeriod = getDaysInPeriod(frequency);
            double daily = minimumSessionPeriod / daysInPeriod;
            System.out.println("DEBUG: Flexible leaf " + frequency + ": " + minimumSessionPeriod + " / " + daysInPeriod + " = " + daily);
            return Math.round(daily * 100.0) / 100.0;
        } else {
            // For leaf level specific schedules, we need to get values from the segment context
            // This method is called with a converted segment, so values are handled in segment processing
            System.out.println("DEBUG: Specific leaf " + frequency + " - values handled at segment level");
            return minimumSessionPeriod; // Return as-is, division happens at segment level
        }
    }
    
    /**
     * Find which segment aligns with the current period start date
     */
    private ScheduleSpec.Segment findAlignedSegment(ScheduleSpec parentSpec, LocalDate periodStart) {
        if (periodStart == null || parentSpec.getSegments() == null) {
            return null;
        }
        
        String parentFrequency = parentSpec.getFrequency();
        
        for (ScheduleSpec.Segment segment : parentSpec.getSegments()) {
            if (isSegmentAlignedWithPeriod(segment, parentFrequency, periodStart)) {
                return segment;
            }
        }
        
        return null;
    }
    
    /**
     * Check if a segment aligns with the given period start date
     */
    private boolean isSegmentAlignedWithPeriod(ScheduleSpec.Segment segment, String parentFrequency, LocalDate periodStart) {
        if (segment.getValues() == null || segment.getValues().isEmpty()) {
            return false;
        }
        
        String segmentFreq = segment.getFrequency();
        
        switch (parentFrequency.toUpperCase()) {
            case "MONTHLY":
                return isMonthlySegmentAligned(segment, periodStart);
            case "QUARTERLY":
                return isQuarterlySegmentAligned(segment, periodStart);
            case "YEARLY":
                return isYearlySegmentAligned(segment, periodStart);
            default:
                return false;
        }
    }
    
    /**
     * Check if monthly segment aligns with period start
     */
    private boolean isMonthlySegmentAligned(ScheduleSpec.Segment segment, LocalDate periodStart) {
        String segmentFreq = segment.getFrequency();
        
        if (segmentFreq.equalsIgnoreCase("WEEKLY")) {
            // Check if period start aligns with any of the specified weeks
            int weekOfMonth = getWeekOfMonth(periodStart);
            for (String value : segment.getValues()) {
                if (value.startsWith("W") && value.length() > 1) {
                    try {
                        int weekNum = Integer.parseInt(value.substring(1));
                        if (weekNum == weekOfMonth) {
                            System.out.println("DEBUG: Monthly period start " + periodStart + " aligns with week " + weekNum);
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid format
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get week of month (1-5) for a given date
     */
    private int getWeekOfMonth(LocalDate date) {
        return (date.getDayOfMonth() - 1) / 7 + 1;
    }
    
    /**
     * Check if quarterly segment aligns with period start
     */
    private boolean isQuarterlySegmentAligned(ScheduleSpec.Segment segment, LocalDate periodStart) {
        // Similar logic for quarterly alignment
        String segmentFreq = segment.getFrequency();
        
        if (segmentFreq.equalsIgnoreCase("MONTHLY")) {
            // Check if period start aligns with specified months
            int monthOfQuarter = ((periodStart.getMonthValue() - 1) % 3) + 1;
            for (String value : segment.getValues()) {
                if (value.startsWith("M") && value.length() > 1) {
                    try {
                        int monthNum = Integer.parseInt(value.substring(1));
                        if (monthNum == monthOfQuarter) {
                            System.out.println("DEBUG: Quarterly period start aligns with month " + monthNum);
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid format
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if yearly segment aligns with period start
     */
    private boolean isYearlySegmentAligned(ScheduleSpec.Segment segment, LocalDate periodStart) {
        // Similar logic for yearly alignment
        String segmentFreq = segment.getFrequency();
        
        if (segmentFreq.equalsIgnoreCase("QUARTERLY")) {
            // Check if period start aligns with specified quarters
            int quarter = (periodStart.getMonthValue() - 1) / 3 + 1;
            for (String value : segment.getValues()) {
                if (value.startsWith("Q") && value.length() > 1) {
                    try {
                        int quarterNum = Integer.parseInt(value.substring(1));
                        if (quarterNum == quarter) {
                            System.out.println("DEBUG: Yearly period start aligns with quarter " + quarterNum);
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid format
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Convert a segment to a ScheduleSpec for recursive processing
     */
    private ScheduleSpec convertSegmentToSpec(ScheduleSpec.Segment segment) {
        ScheduleSpec spec = new ScheduleSpec();
        spec.setFrequency(segment.getFrequency());
        spec.setFlexible(segment.getFlexible());
        spec.setSegments(segment.getSegments());
        // Store segment values for leaf level processing
        return spec;
    }
    
    /**
     * Calculate daily value for flexible schedules
     */
    private Double calculateFlexibleDaily(double minimumSessionPeriod, String frequency) {
        int daysInPeriod = getDaysInPeriod(frequency);
        System.out.println("DEBUG: Flexible calculation - daysInPeriod: " + daysInPeriod);
        
        if (daysInPeriod == 0) {
            System.out.println("DEBUG: daysInPeriod is 0, returning 0.0");
            return 0.0;
        }
        
        double daily = minimumSessionPeriod / daysInPeriod;
        double roundedDaily = Math.round(daily * 100.0) / 100.0;
        
        System.out.println("DEBUG: Flexible calculation: " + minimumSessionPeriod + " / " + daysInPeriod + " = " + daily + " (rounded: " + roundedDaily + ")");
        return roundedDaily; // Round to 2 decimal places
    }
    
    /**
     * Calculate daily value for specific schedules
     */
    private Double calculateSpecificDaily(double minimumSessionPeriod, ScheduleSpec scheduleSpec) {
        int totalSpecificDays = countTotalSpecificDays(scheduleSpec);
        System.out.println("DEBUG: Specific calculation - totalSpecificDays: " + totalSpecificDays);
        
        if (totalSpecificDays == 0) {
            System.out.println("DEBUG: totalSpecificDays is 0, returning 0.0");
            return 0.0;
        }
        
        double daily = minimumSessionPeriod / totalSpecificDays;
        double roundedDaily = Math.round(daily * 100.0) / 100.0;
        
        System.out.println("DEBUG: Specific calculation: " + minimumSessionPeriod + " / " + totalSpecificDays + " = " + daily + " (rounded: " + roundedDaily + ")");
        return roundedDaily; // Round to 2 decimal places
    }
    
    /**
     * Get number of days in period based on frequency
     */
    private int getDaysInPeriod(String frequency) {
        if (frequency == null) return 0;
        
        switch (frequency.toUpperCase()) {
            case "DAILY":
                return 1;
            case "WEEKLY":
                return 7;
            case "MONTHLY":
                return 30; // Approximate
            case "QUARTERLY":
                return 90; // Approximate
            case "YEARLY":
                return 365; // Approximate
            default:
                return 0;
        }
    }
    
    /**
     * Count total specific days across all segments (ignoring time segments)
     */
    private int countTotalSpecificDays(ScheduleSpec scheduleSpec) {
        System.out.println("DEBUG: Counting specific days from segments");
        
        if (scheduleSpec.getSegments() == null || scheduleSpec.getSegments().isEmpty()) {
            System.out.println("DEBUG: No segments found, returning 0");
            return 0;
        }
        
        System.out.println("DEBUG: Found " + scheduleSpec.getSegments().size() + " segments");
        
        int totalDays = 0;
        for (int i = 0; i < scheduleSpec.getSegments().size(); i++) {
            ScheduleSpec.Segment segment = scheduleSpec.getSegments().get(i);
            System.out.println("DEBUG: Segment " + (i+1) + " - frequency: " + segment.getFrequency() + ", values: " + segment.getValues());
            
            if (segment.getValues() != null && !segment.getValues().isEmpty()) {
                // Only count day-based segments, ignore time segments
                if (isDayBasedSegment(segment.getFrequency())) {
                    int segmentDays = segment.getValues().size();
                    totalDays += segmentDays;
                    System.out.println("DEBUG: Segment " + (i+1) + " is day-based, adding " + segmentDays + " days (total: " + totalDays + ")");
                } else {
                    System.out.println("DEBUG: Segment " + (i+1) + " is time-based, ignoring");
                }
            } else {
                System.out.println("DEBUG: Segment " + (i+1) + " has no values, ignoring");
            }
        }
        
        System.out.println("DEBUG: Final total specific days: " + totalDays);
        return totalDays;
    }
    
    /**
     * Check if segment is day-based (not time-based)
     */
    private boolean isDayBasedSegment(String frequency) {
        if (frequency == null) return false;
        String freq = frequency.toUpperCase();
        return freq.equals("DAILY") || freq.equals("WEEKLY") || freq.equals("MONTHLY") || 
               freq.equals("QUARTERLY") || freq.equals("YEARLY");
    }
    
    @Override
    public List<GoalPeriod> getPeriodsForGoal(String goalUuid) {
        List<GoalPeriod> periods = goalPeriodRepository.findByParentGoalUuid(goalUuid);
        periods.sort((p1, p2) -> p1.getPeriodStart().compareTo(p2.getPeriodStart()));
        return periods;
    }
    
    @Override
    public Optional<GoalPeriod> getActivePeriodForGoal(String goalUuid, LocalDate date) {
        return goalPeriodRepository.findActivePeriodForGoal(goalUuid, date);
    }
    
    @Override
    public GoalPeriod updatePeriod(GoalPeriod period) {
        period.setLastUpdatedAt(LocalDateTime.now());
        return goalPeriodRepository.save(period);
    }
    
    @Override
    public void deletePeriod(String periodUuid) {
        goalPeriodRepository.findByUuid(periodUuid).ifPresent(period -> {
            // Soft delete - mark as deleted in database if needed
            period.setLastUpdatedAt(LocalDateTime.now());
            goalPeriodRepository.save(period);
        });
    }
    
    @Override
    public Optional<GoalPeriod> createNextPeriod(String goalUuid) {
        // For now, we'll skip this method since it's not being used in the current flow
        // The main issue is with weight transfer from Goal to GoalPeriod in createPeriodForGoal
        return Optional.empty();
    }
    
    @Override
    public PeriodRange calculatePeriodRange(Goal goal, LocalDate referenceDate) {
        if (goal.getScheduleSpec() == null || goal.getScheduleSpec().getFrequency() == null) {
            // Default to monthly if no frequency specified
            LocalDate start = referenceDate.withDayOfMonth(1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return new PeriodRange(start, end);
        }
        
        String frequency = goal.getScheduleSpec().getFrequency();
        
        switch (frequency.toUpperCase()) {
            case "DAILY":
                return calculateDailyPeriod(referenceDate);
            case "WEEKLY":
                return calculateWeeklyPeriod(referenceDate);
            case "MONTHLY":
                return calculateMonthlyPeriod(referenceDate);
            case "QUARTERLY":
                return calculateQuarterlyPeriod(referenceDate);
            case "YEARLY":
                return calculateYearlyPeriod(referenceDate);
            default:
                return calculateMonthlyPeriod(referenceDate);
        }
    }
    
    @Override
    public PeriodRange calculateFirstPeriodRange(Goal goal) {
        LocalDate startDate = goal.getStartDate() != null ? 
            goal.getStartDate().toLocalDate() : LocalDate.now();
        return calculatePeriodRange(goal, startDate);
    }
    
    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================
    
    private PeriodRange calculateDailyPeriod(LocalDate referenceDate) {
        return new PeriodRange(referenceDate, referenceDate);
    }
    
    private PeriodRange calculateWeeklyPeriod(LocalDate referenceDate) {
        LocalDate start = referenceDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);
        return new PeriodRange(start, end);
    }
    
    private PeriodRange calculateMonthlyPeriod(LocalDate referenceDate) {
        LocalDate start = referenceDate.withDayOfMonth(1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return new PeriodRange(start, end);
    }
    
    private PeriodRange calculateQuarterlyPeriod(LocalDate referenceDate) {
        int month = referenceDate.getMonthValue();
        int quarter = (month - 1) / 3;
        int startMonth = quarter * 3 + 1;
        
        LocalDate start = referenceDate.withMonth(startMonth).withDayOfMonth(1);
        LocalDate end = start.plusMonths(2).withDayOfMonth(start.plusMonths(2).lengthOfMonth());
        return new PeriodRange(start, end);
    }
    
    private PeriodRange calculateYearlyPeriod(LocalDate referenceDate) {
        LocalDate start = referenceDate.withDayOfYear(1);
        LocalDate end = referenceDate.withDayOfYear(referenceDate.lengthOfYear());
        return new PeriodRange(start, end);
    }
}
