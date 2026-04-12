package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Service.Interface.SmartTodoService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalService;
import com.sagarpandey.activity_tracker.Service.Interface.ActivityServiceInterface;
import com.sagarpandey.activity_tracker.dtos.SmartTodoResponse;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.enums.ScheduleType;
import com.sagarpandey.activity_tracker.enums.ScheduleDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Todo Service - Mature Production Implementation
 * 
 * Provides intelligent daily task generation that answers "What should I do TODAY?"
 * 
 * Features:
 * - Schedule-aware filtering (FLEXIBLE vs SPECIFIC_DAYS)
 * - Priority-based sorting with multi-level criteria
 * - Deterministic urgency detection (streak risk, behind schedule)
 * - Real activity data integration for progress tracking
 * - Time-based priority considerations
 */
@Service
public class SmartTodoServiceV1 implements SmartTodoService {
    
    private static final Logger log = LoggerFactory.getLogger(SmartTodoServiceV1.class);
    
    private final GoalService goalService;
    private final ActivityServiceInterface activityService;
    
    @Autowired
    public SmartTodoServiceV1(GoalService goalService, ActivityServiceInterface activityService) {
        this.goalService = goalService;
        this.activityService = activityService;
    }
    
    @Override
    public List<SmartTodoResponse> getTodaySmartTodos(String userId) {
        try {
            List<GoalResponse> allGoals = goalService.getAllGoalsByUser(userId);
            LocalDate today = LocalDate.now();
            DayOfWeek todayDayOfWeek = today.getDayOfWeek();
            
            // Fetch all activities for this user (cache for processing)
            List<ActivityResponse> allActivities = activityService.readAll();
            
            List<SmartTodoResponse> smartTodos = new ArrayList<>();
            
            for (GoalResponse goalResponse : allGoals) {
                // Skip null goals and parent goals
                if (goalResponse == null || 
                    (goalResponse.getIsLeaf() != null && !goalResponse.getIsLeaf())) {
                    continue;
                }
                
                // Check if goal should appear today based on schedule
                if (!isScheduledForToday(goalResponse, todayDayOfWeek)) {
                    continue;
                }
                
                SmartTodoResponse todo = buildTodoItem(goalResponse, today, allActivities);
                smartTodos.add(todo);
            }
            
            // Sort by intelligent priority system
            return sortSmartTodos(smartTodos);
            
        } catch (Exception e) {
            log.error("Error generating smart todos for userId={}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
        
    @Override
    public List<SmartTodoResponse> refreshTodayTodos(String userId) {
        return getTodaySmartTodos(userId);
    }
    
    // ===================================================================
    // CORE SMART TODO BUILDING LOGIC
    // ===================================================================
    
    /**
     * Determines if a goal should be included in today's todo list
     * based on schedule configuration
     */
    private boolean isScheduledForToday(GoalResponse goal, DayOfWeek todayDayOfWeek) {
        ScheduleType scheduleType = goal.getScheduleType();
        
        if (scheduleType == null || scheduleType == ScheduleType.FLEXIBLE) {
            return true; // Flexible goals always appear
        }
        
        if (scheduleType == ScheduleType.SPECIFIC_DAYS) {
            // Check if today is in the scheduled days
            List<ScheduleDay> scheduledDays = goal.getScheduleDays();
            if (scheduledDays == null || scheduledDays.isEmpty()) {
                return true; // No days configured, default to include
            }
            
            ScheduleDay todayScheduleDay = ScheduleDay.valueOf(todayDayOfWeek.name());
            return scheduledDays.contains(todayScheduleDay);
        }
        
        return true; // Default to include if schedule type unknown
    }
    
    /**
     * Builds a complete SmartTodoResponse for a single goal
     */
    private SmartTodoResponse buildTodoItem(GoalResponse goal, LocalDate today, List<ActivityResponse> allActivities) {
        SmartTodoResponse todo = new SmartTodoResponse();
        
        // 1. Basic goal information
        todo.setGoalId(goal.getId());
        todo.setTitle(goal.getTitle());
        todo.setDescription(goal.getDescription());
        todo.setPriority(goal.getPriority());
        todo.setGoalType(goal.getGoalType());
        todo.setPriorityDisplay("P" + (goal.getPriority().ordinal() + 1));
        
        // 2. Schedule information
        todo.setScheduleType(goal.getScheduleType() != null ? 
            goal.getScheduleType().toString() : "FLEXIBLE");
        todo.setScheduledForToday(true); // Already filtered by isScheduledForToday()
        
        // 3. Progress tracking (real data)
        int todayProgress = (int) countTodayActivities(goal.getId(), today, allActivities);
        int todayTarget = calculateTodayTarget(goal);
        todo.setCurrentProgress(todayProgress);
        todo.setTargetProgress(todayTarget);
        todo.setProgressPercentage((todayTarget > 0) ? 
            (todayProgress * 100.0 / todayTarget) : 0.0);
        todo.setCompletedToday(todayProgress >= todayTarget);
        
        // 4. Streak and urgency detection
        todo.setCurrentStreak(goal.getCurrentStreak());
        
        boolean streakAtRisk = isStreakAtRisk(goal, today, allActivities);
        todo.setStreakAtRisk(streakAtRisk);
        
        boolean behindSchedule = isBehindSchedule(goal, today, allActivities);
        todo.setBehindSchedule(behindSchedule);
        
        String urgency = buildUrgencyReason(goal, streakAtRisk, behindSchedule);
        todo.setUrgencyReason(urgency);
        
        // 5. Time requirements
        if (goal.getMinimumSessionPeriod() != null) {
            todo.setMinimumSessionPeriod(goal.getMinimumSessionPeriod());
        }
        if (goal.getMinimumSessionDaily() != null) {
            todo.setMinimumSessionDaily(goal.getMinimumSessionDaily());
        }
        
        Integer suggestedTime = calculateSuggestedTime(goal);
        todo.setSuggestedTimeMinutes(suggestedTime);
        
        // 6. Last activity info
        LocalDateTime lastActivityDate = getLastActivityDate(goal.getId(), allActivities);
        if (lastActivityDate != null) {
            todo.setLastCompletedDate(lastActivityDate.toLocalDate().toString());
        }
        
        // 7. Quick log context
        todo.setRequiresQuickLog(todayProgress < todayTarget);
        todo.setQuickLogContext(goal.getTitle());
        
        return todo;
    }
    
    /**
     * Multi-level intelligent sorting for todo priority
     * 
     * Sort order:
     * 1. By goal priority (P1 < P2 < P3 < P4)
     * 2. Streak at risk (boosted to top if risk exists)
     * 3. Behind schedule (important for pace management)
     * 4. Scheduled for today (vs not scheduled)
     * 5. Incomplete first (vs already completed today)
     */
    private List<SmartTodoResponse> sortSmartTodos(List<SmartTodoResponse> todos) {
        return todos.stream()
            .sorted((a, b) -> {
                // Primary: Goal priority (CRITICAL/HIGH/MEDIUM/LOW)
                int priorityCompare = a.getPriority().ordinal() - b.getPriority().ordinal();
                if (priorityCompare != 0) return priorityCompare;
                
                // Secondary: Streak at risk - urgent!
                if (a.isStreakAtRisk() && !b.isStreakAtRisk()) return -1;
                if (b.isStreakAtRisk() && !a.isStreakAtRisk()) return 1;
                
                // Tertiary: Behind schedule - needs attention
                if (a.isBehindSchedule() && !b.isBehindSchedule()) return -1;
                if (b.isBehindSchedule() && !a.isBehindSchedule()) return 1;
                
                // Quaternary: Scheduled for today
                if (a.isScheduledForToday() && !b.isScheduledForToday()) return -1;
                if (b.isScheduledForToday() && !a.isScheduledForToday()) return 1;
                
                // Quinary: Incomplete first (higher priority than already completed)
                if (a.isCompletedToday() && !b.isCompletedToday()) return 1;
                if (b.isCompletedToday() && !a.isCompletedToday()) return -1;
                
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    // ===================================================================
    // DETERMINISTIC URGENCY DETECTION
    // ===================================================================
    
    /**
     * Determines if a goal's streak is at risk TODAY
     * 
     * Streak is at risk if:
     * - Goal has an active streak (>0)
     * - Goal was scheduled yesterday
     * - No activities logged for goal yesterday
     */
    private boolean isStreakAtRisk(GoalResponse goal, LocalDate today, 
                                   List<ActivityResponse> allActivities) {
        // Only relevant if goal has active streak
        if (goal.getCurrentStreak() == null || goal.getCurrentStreak() <= 0) {
            return false;
        }
        
        // Check if goal was scheduled yesterday
        LocalDate yesterday = today.minusDays(1);
        if (!isScheduledForToday(goal, yesterday.getDayOfWeek())) {
            return false; // Not scheduled yesterday, no streak risk
        }
        
        // Check if activities exist from yesterday
        long yesterdayActivities = countTodayActivities(goal.getId(), yesterday, allActivities);
        
        // Streak at risk if no activities were completed yesterday
        return yesterdayActivities == 0;
    }
    
    /**
     * Determines if a goal is currently behind schedule
     * 
     * Behind schedule if:
     * - Has targetFrequencyWeekly configured
     * - Current week shows < 50% of expected progress
     */
    private boolean isBehindSchedule(GoalResponse goal, LocalDate today,
                                     List<ActivityResponse> allActivities) {
        // Only applies to goals with frequency targets
        if (goal.getTargetFrequencyWeekly() == null || goal.getTargetFrequencyWeekly() <= 0) {
            return false;
        }
        
        // Get current week Monday
        LocalDate weekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // Count activities for this week
        long weekActivities = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate dayInWeek = weekMonday.plusDays(i);
            if (dayInWeek.isAfter(today)) break; // Don't count future days
            
            weekActivities += countTodayActivities(goal.getId(), dayInWeek, allActivities);
        }
        
        // Calculate expected progress (days passed this week / 7 * frequency)
        int daysIntoWeek = today.getDayOfWeek().getValue();
        double expectedProgress = (daysIntoWeek / 7.0) * goal.getTargetFrequencyWeekly();
        
        // Behind if less than 50% of expected
        return weekActivities < (expectedProgress * 0.5);
    }
    
    /**
     * Builds human-readable urgency reason for todo item
     */
    private String buildUrgencyReason(GoalResponse goal, boolean streakAtRisk, 
                                      boolean behindSchedule) {
        if (streakAtRisk) {
            return "⚠️ Streak breaks today - complete to maintain streak!";
        }
        if (behindSchedule) {
            return "📉 Behind weekly pace - catch up today!";
        }
        // No special urgency
        return "";
    }
    
    // ===================================================================
    // ACTIVITY DATA PROCESSING
    // ===================================================================
    
    /**
     * Counts activities for a specific goal on a specific date
     */
    private long countTodayActivities(Long goalId, LocalDate date, 
                                     List<ActivityResponse> allActivities) {
        if (goalId == null || allActivities == null) {
            return 0;
        }
        
        return allActivities.stream()
            .filter(activity -> activity.getGoalId() != null && 
                              activity.getGoalId().equals(goalId))
            .filter(activity -> isActivityOnDate(activity, date))
            .count();
    }
    
    /**
     * Checks if an activity occurred on the given date
     */
    private boolean isActivityOnDate(ActivityResponse activity, LocalDate date) {
        try {
            // Try startTime first (preferred)
            if (activity.getStartTime() != null) {
                LocalDate activityDate = activity.getStartTime().toLocalDate();
                return activityDate.equals(date);
            }
            
            // Fallback to created_at
            if (activity.getCreated_at() != null) {
                String dateStr = activity.getCreated_at();
                if (dateStr.length() >= 10) {
                    LocalDate activityDate = LocalDate.parse(dateStr.substring(0, 10));
                    return activityDate.equals(date);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse activity date: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Gets the most recent activity date for a goal
     */
    private LocalDateTime getLastActivityDate(Long goalId, List<ActivityResponse> allActivities) {
        if (goalId == null || allActivities == null || allActivities.isEmpty()) {
            return null;
        }
        
        return allActivities.stream()
            .filter(activity -> activity.getGoalId() != null && 
                              activity.getGoalId().equals(goalId))
            .filter(activity -> activity.getStartTime() != null)
            .map(activity -> activity.getStartTime().atZoneSameInstant(
                java.time.ZoneId.systemDefault()).toLocalDateTime())
            .max(LocalDateTime::compareTo)
            .orElse(null);
    }
    
    // ===================================================================
    // TARGET AND TIME CALCULATIONS
    // ===================================================================
    
    /**
     * Calculates the target progress count for today based on goal configuration
     * 
     * FLEXIBLE goals: targetFrequencyWeekly / 7 (spread evenly)
     * SPECIFIC_DAYS goals: 1 (if today is scheduled), 0 (if not)
     */
    private int calculateTodayTarget(GoalResponse goal) {
        if (goal.getScheduleType() == ScheduleType.SPECIFIC_DAYS) {
            // For specific days, target is 1 if scheduled, 0 if not
            // (already filtered to only scheduled days, so always 1)
            return 1;
        }
        
        // FLEXIBLE goals
        if (goal.getTargetFrequencyWeekly() != null && goal.getTargetFrequencyWeekly() > 0) {
            // Spread weekly target across 7 days
            return Math.max(1, goal.getTargetFrequencyWeekly() / 7);
        }
        
        // Default target if nothing configured
        return 1;
    }
    
    /**
     * Calculates suggested time for this goal
     * 
     * Considers:
     * - minimumSessionDaily (auto-calculated from period)
     * - minimumSessionPeriod (total for period)
     * - Goal type adjustments (PROJECT +15 min, SKILL +30 min)
     */
    private Integer calculateSuggestedTime(GoalResponse goal) {
        Integer suggestedTime = 30; // Default 30 minutes
        
        // Use minimumSessionDaily if available (preferred)
        if (goal.getMinimumSessionDaily() != null && goal.getMinimumSessionDaily() > 0) {
            suggestedTime = goal.getMinimumSessionDaily();
        } else if (goal.getMinimumSessionPeriod() != null && goal.getMinimumSessionPeriod() > 0) {
            // Estimate daily from period
            suggestedTime = Math.max(suggestedTime, goal.getMinimumSessionPeriod() / 7);
        }
        
        // Adjust based on goal type
        if (goal.getGoalType() == GoalType.PROJECT) {
            suggestedTime = Math.max(suggestedTime, 45); // Project needs more time
        } else if (goal.getGoalType() == GoalType.SKILL) {
            suggestedTime = Math.max(suggestedTime, 60); // Skill needs extended session
        }
        
        return suggestedTime;
    }
    
    /**
     * Utility method to parse schedule days from DTO
     */
    @SuppressWarnings("unused")
    private Set<ScheduleDay> parseScheduleDays(List<ScheduleDay> scheduleDaysList) {
        if (scheduleDaysList == null || scheduleDaysList.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(scheduleDaysList);
    }
    
    @SuppressWarnings("unused")
    private int getTodayActivitiesCount(Long goalId) {
        try {
            List<ActivityResponse> allActivities = activityService.readAll();
            LocalDate today = LocalDate.now();
            
            return (int) countTodayActivities(goalId, today, allActivities);
        } catch (Exception e) {
            log.error("Error counting today's activities for goalId={}: {}", goalId, e.getMessage());
            return 0;
        }
    }
    
    @SuppressWarnings("unused")
    private String getLastActivityDate(Long goalId) {
        try {
            List<ActivityResponse> allActivities = activityService.readAll();
            LocalDateTime lastDate = getLastActivityDate(goalId, allActivities);
            return lastDate != null ? lastDate.toLocalDate().toString() : "";
        } catch (Exception e) {
            log.error("Error getting last activity date for goalId={}: {}", goalId, e.getMessage());
            return "";
        }
    }
}
