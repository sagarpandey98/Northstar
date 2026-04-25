package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Service.Interface.SmartTodoService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalService;
import com.sagarpandey.activity_tracker.Service.Interface.ActivityServiceInterface;
import com.sagarpandey.activity_tracker.dtos.SmartTodoResponse;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.enums.GoalType;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import com.sagarpandey.activity_tracker.utils.ScheduleSpecEvaluator;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private final GoalPeriodRepository goalPeriodRepository;
    
    @Autowired
    public SmartTodoServiceV1(GoalService goalService, ActivityServiceInterface activityService, GoalPeriodRepository goalPeriodRepository) {
        this.goalService = goalService;
        this.activityService = activityService;
        this.goalPeriodRepository = goalPeriodRepository;
    }
    
    @Override
    public List<SmartTodoResponse> getTodaySmartTodos(String userId) {
        try {
            List<GoalResponse> allGoals = goalService.getAllGoalsByUser(userId);
            
            // 1. Determine "User's Today" using their configured timezone
            // We look for a timezone in any of their scheduleSpecs, defaulting to UTC if none found.
            ZoneId userZone = ZoneOffset.UTC;
            for (GoalResponse g : allGoals) {
                if (g.getScheduleSpec() != null && g.getScheduleSpec().getTimezone() != null) {
                    try {
                        userZone = ZoneId.of(g.getScheduleSpec().getTimezone());
                        break; 
                    } catch (Exception e) {
                        // Ignore invalid zone IDs
                    }
                }
            }
            LocalDate userToday = LocalDate.now(userZone);
            
            // 2. Fetch activities for THIS user
            List<ActivityResponse> allActivities = activityService.readAll(userId);
            
            List<SmartTodoResponse> smartTodos = new ArrayList<>();
            
            for (GoalResponse goalResponse : allGoals) {
                if (!isEligibleForDate(goalResponse, userToday)) {
                    continue;
                }
                
                // Build item using the timezone-accurate userToday
                SmartTodoResponse todo = buildTodoItem(goalResponse, userToday, allActivities);
                
                // Show if scheduled OR if progress was made OR period starts today.
                if (todo.isScheduledForToday()
                        || todo.getCurrentProgress() > 0
                        || hasPeriodStartingOnDate(goalResponse, userToday)) {
                    smartTodos.add(todo);
                }
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
    
    @Override
    public List<SmartTodoResponse> getSmartTodosForDate(String userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        try {
            List<GoalResponse> allGoals = goalService.getAllGoalsByUser(userId);
            List<ActivityResponse> allActivities = activityService.readAll(userId);
            List<SmartTodoResponse> smartTodos = new ArrayList<>();
            
            for (GoalResponse goalResponse : allGoals) {
                if (!isEligibleForDate(goalResponse, targetDate)) {
                    continue;
                }
                
                SmartTodoResponse todo = buildTodoItem(goalResponse, targetDate, allActivities);
                if (todo.isScheduledForToday()
                        || todo.getCurrentProgress() > 0
                        || hasPeriodStartingOnDate(goalResponse, targetDate)) {
                    smartTodos.add(todo);
                }
            }
            
            return sortSmartTodos(smartTodos);
        } catch (Exception e) {
            log.error("Error generating smart todos for userId={} and date={}: {}", userId, targetDate, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // ===================================================================
    // CORE SMART TODO BUILDING LOGIC
    // ===================================================================
    
    /**
     * Determines if a goal should be included in today's todo list
     * based on schedule configuration
     */
    private boolean isScheduledForToday(GoalResponse goal, LocalDate today) {
        // 1. Phase 4 Rule Engine (ScheduleSpec)
        if (goal.getScheduleSpec() != null) {
            // If spec exists, it is the source of truth
            return ScheduleSpecEvaluator.isActionable(goal.getScheduleSpec(), today);
        }
        
        // Default to true for FLEXIBLE or undefined
        return true;
    }
    
    private boolean isTrackableGoal(GoalResponse goal) {
        if (goal == null) {
            return false;
        }
        if (Boolean.TRUE.equals(goal.getIsMilestone())) {
            return false;
        }
        return goal.getIsLeaf() == null || goal.getIsLeaf();
    }
    
    private boolean isEligibleForDate(GoalResponse goal, LocalDate date) {
        if (!isTrackableGoal(goal)) {
            return false;
        }
        if (goal.getUuid() == null || date == null) {
            return true;
        }
        
        List<GoalPeriod> periods = goalPeriodRepository.findByParentGoalUuid(goal.getUuid());
        if (periods == null || periods.isEmpty()) {
            return true;
        }
        
        return goalPeriodRepository.findActivePeriodForGoal(goal.getUuid(), date).isPresent();
    }
    
    private boolean hasPeriodStartingOnDate(GoalResponse goal, LocalDate date) {
        if (goal == null || goal.getUuid() == null || date == null) {
            return false;
        }
        List<GoalPeriod> periods = goalPeriodRepository.findByParentGoalUuid(goal.getUuid());
        if (periods == null || periods.isEmpty()) {
            return false;
        }
        return periods.stream().anyMatch(period -> date.equals(period.getPeriodStart()));
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
        todo.setScheduleType(goal.getScheduleSpec() != null ? "CONFIGURED" : "FLEXIBLE");
        todo.setScheduledForToday(isScheduledForToday(goal, today));
        
        // 3. Progress tracking (real data)
        int todayProgress = (int) countTodayActivities(goal.getId(), today, allActivities);
        int todayTarget = calculateTodayTarget(goal, today);
        
        todo.setCurrentProgress(todayProgress);
        todo.setTargetProgress(todayTarget);
        todo.setProgressPercentage((todayTarget > 0) ? (todayProgress * 100.0 / todayTarget) : 0.0);
        
        // Force completedToday to true if progress was made
        todo.setCompletedToday(todayProgress > 0);
        
        // 4. Streak and urgency detection
        todo.setCurrentStreak(goal.getCurrentStreak());
        
        boolean streakAtRisk = isStreakAtRisk(goal, today, allActivities);
        todo.setStreakAtRisk(streakAtRisk);
        
        boolean behindSchedule = isBehindSchedule(goal, today, allActivities);
        todo.setBehindSchedule(behindSchedule);
        
        String urgency = buildUrgencyReason(goal, streakAtRisk, behindSchedule);
        todo.setUrgencyReason(urgency);
        
        // 5. Time requirements
        todo.setMinimumSessionPeriod(goal.getMinimumTimeCommittedPeriod());
        todo.setMinimumSessionDaily(goal.getMinimumTimeCommittedDaily());
        todo.setSuggestedTimeMinutes(calculateSuggestedTime(goal, today));
        
        // 6. SMART PRIORITY CALCULATION
        double score = calculateUrgencyScore(todo);
        todo.setUrgencyScore(score);
        todo.setSmartPriorityGroup(determinePriorityGroup(score, todo));
        
        // 7. Last activity info
        LocalDateTime lastActivityDate = getLastActivityDate(goal.getId(), allActivities);
        if (lastActivityDate != null) {
            todo.setLastCompletedDate(lastActivityDate.toLocalDate().toString());
        }
        
        // 8. Quick log context
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
    /**
     * Intelligent sorting for todo priority using Smart Urgency Score
     */
    private List<SmartTodoResponse> sortSmartTodos(List<SmartTodoResponse> todos) {
        return todos.stream()
            .sorted((a, b) -> {
                // 1. Incomplete first
                if (a.isCompletedToday() != b.isCompletedToday()) {
                    return a.isCompletedToday() ? 1 : -1;
                }
                
                // 2. Sort by smart urgency score (Highest score first for incomplete)
                if (a.getUrgencyScore() != null && b.getUrgencyScore() != null) {
                    return b.getUrgencyScore().compareTo(a.getUrgencyScore()) * -1;
                }
                
                // Fallback to legacy priority
                return b.getPriority().compareTo(a.getPriority());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates a dynamic urgency score (0-100) based on multiple factors
     */
    private double calculateUrgencyScore(SmartTodoResponse todo) {
        // Base score by priority: CRITICAL: 40, HIGH: 30, MEDIUM: 20, LOW: 10
        double score = (todo.getPriority().ordinal() + 1) * 10.0;
        
        // Multipliers/Addons
        if (todo.isStreakAtRisk()) score += 25;    // Streak is top priority
        if (todo.isBehindSchedule()) score += 15;  // Falling behind is next
        
        // Suggested time influence (Complexity/Commitment)
        if (todo.getSuggestedTimeMinutes() != null) {
            if (todo.getSuggestedTimeMinutes() >= 60) score += 10;
            else if (todo.getSuggestedTimeMinutes() >= 30) score += 5;
        }
        
        // Scheduled vs Unscheduled (bonus for scheduled today)
        if (todo.isScheduledForToday()) score += 10;
        
        return Math.min(100.0, score);
    }
    
    private String determinePriorityGroup(double score, SmartTodoResponse todo) {
        if (todo.isCompletedToday()) return "COMPLETED";
        if (score >= 80) return "CRITICAL PACE";
        if (score >= 60) return "URGENT";
        if (score >= 40) return "IMPORTANT";
        return "DAILY TRACK";
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
        if (!isScheduledForToday(goal, yesterday)) {
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
     * - Has a schedule requirement configured
     * - Current active period shows < 50% of expected check-in pace
     */
    private boolean isBehindSchedule(GoalResponse goal, LocalDate today,
                                     List<ActivityResponse> allActivities) {
        Integer minCheckins = getMinCheckinsRequired(goal.getScheduleSpec());
        if (minCheckins == null || minCheckins <= 0 || goal.getUuid() == null) {
            return false;
        }

        Optional<GoalPeriod> activePeriodOpt = goalPeriodRepository.findActivePeriodForGoal(goal.getUuid(), today);
        if (activePeriodOpt.isEmpty()) {
            return false;
        }

        GoalPeriod period = activePeriodOpt.get();
        int totalActionableDays = ScheduleSpecEvaluator.countActionableDays(
            period.getPeriodStart(), period.getPeriodEnd(), goal.getScheduleSpec()
        );
        int elapsedActionableDays = ScheduleSpecEvaluator.countActionableDays(
            period.getPeriodStart(), today, goal.getScheduleSpec()
        );
        if (totalActionableDays <= 0 || elapsedActionableDays <= 0) {
            return false;
        }

        long actualCheckins = countActivitiesBetween(goal.getId(), period.getPeriodStart(), today, allActivities);
        double expectedByToday = minCheckins * (elapsedActionableDays / (double) totalActionableDays);
        return actualCheckins < (expectedByToday * 0.5);
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
    private boolean isActivityOnDate(ActivityResponse activity, LocalDate targetDate) {
        try {
            // Check startTime (OffsetDateTime)
            if (activity.getStartTime() != null) {
                // We compare the LocalDate of the activity in ITS OWN offset.
                // However, if the user is in +05:30 and it's 01:00 AM April 15, 
                // and the server is UTC April 14, targetDate will be April 14.
                // We should match if it's "close enough" or if the activity's local date matches targetDate.
                LocalDate activityLocal = activity.getStartTime().toLocalDate();
                
                if (activityLocal.equals(targetDate)) return true;
                
                // Special case for UTC servers: if user is ahead of server, 
                // 'today' on server might be yesterday for user.
                // We check if the activity happened in the last 24 hours relative to "now".
                OffsetDateTime activityTime = activity.getStartTime();
                OffsetDateTime now = OffsetDateTime.now(activityTime.getOffset());
                if (Duration.between(activityTime, now).toHours() < 24 && activityTime.isBefore(now.plusMinutes(5))) {
                    // Logic: If the activity is very recent (last 24h), and the goal is daily tracking,
                    // we count it towards "today's" status in the UI to give immediate feedback.
                    return true;
                }
                
                return false;
            }
            
            // Fallback to created_at
            if (activity.getCreated_at() != null) {
                String dateStr = activity.getCreated_at();
                if (dateStr.length() >= 10) {
                    LocalDate activityDate = LocalDate.parse(dateStr.substring(0, 10));
                    return activityDate.equals(targetDate);
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
 * Configured schedules default to one check-in per actionable day.
 */
private int calculateTodayTarget(GoalResponse goal, LocalDate today) {
    ScheduleSpec spec = goal.getScheduleSpec();
    Integer minCheckins = getMinCheckinsRequired(spec);
    if (minCheckins == null || minCheckins <= 0 || goal.getUuid() == null) {
        return 1;
    }

    Optional<GoalPeriod> activePeriodOpt = goalPeriodRepository.findActivePeriodForGoal(goal.getUuid(), today);
    if (activePeriodOpt.isEmpty()) {
        return 1;
    }

    GoalPeriod period = activePeriodOpt.get();
    int actionableDays = ScheduleSpecEvaluator.countActionableDays(period.getPeriodStart(), period.getPeriodEnd(), spec);
    if (actionableDays <= 0) {
        return 1;
    }

    return Math.max(1, (int) Math.ceil(minCheckins / (double) actionableDays));
}

private Integer getMinCheckinsRequired(ScheduleSpec spec) {
    if (spec == null || spec.getRequirements() == null) {
        return null;
    }
    return spec.getRequirements().getMinCheckins();
}

private long countActivitiesBetween(Long goalId, LocalDate start, LocalDate end, List<ActivityResponse> allActivities) {
    if (goalId == null || start == null || end == null || allActivities == null) {
        return 0;
    }
    if (end.isBefore(start)) {
        return 0;
    }

    long count = 0;
    LocalDate current = start;
    while (!current.isAfter(end)) {
        count += countTodayActivities(goalId, current, allActivities);
        current = current.plusDays(1);
    }
    return count;
}

/**
 * Calculates suggested time for this goal
 * 
 * Considers:
 * - minimumSessionDaily (auto-calculated from period)
 * - minimumSessionPeriod (total for period)
 * - Goal type adjustments (PROJECT +15 min, SKILL +30 min)
 */
private Integer calculateSuggestedTime(GoalResponse goal, LocalDate today) {
    Optional<GoalPeriod> activePeriodOpt = goalPeriodRepository.findActivePeriodForGoal(goal.getUuid(), today);

    if (activePeriodOpt.isPresent()) {
        GoalPeriod period = activePeriodOpt.get();
        
        // If the goal has an explicit time commitment, honor it regardless of metric
        Integer periodMin = period.getMinimumSessionPeriod();
        if (periodMin == null) periodMin = goal.getMinimumSessionPeriod();
        
        Double dailyMin = period.getMinimumSessionDaily();
        if (dailyMin == null) dailyMin = goal.getMinimumTimeCommittedDaily() != null ? goal.getMinimumTimeCommittedDaily().doubleValue() : null;

        if (dailyMin != null && dailyMin > 0) {
            return dailyMin.intValue();
        }

        if (periodMin != null && periodMin > 0) {
            double spent = period.getCurrentValue() != null && period.getMetric() == com.sagarpandey.activity_tracker.models.Goal.Metric.DURATION 
                            ? period.getCurrentValue() : 0.0;
            double remaining = Math.max(0, periodMin - spent);
            
            int remainingDays = ScheduleSpecEvaluator.countActionableDays(
                today, period.getPeriodEnd(), goal.getScheduleSpec()
            );
            
            if (remainingDays > 0) {
                return (int) Math.ceil(remaining / remainingDays);
            } else {
                if (periodMin != null && periodMin > 0) {
                    return periodMin;
                }
            }
        }

        // If no explicit commitment, follow default logic
        return calculateDefaultSuggestedTime(goal);
    }
    
    return calculateDefaultSuggestedTime(goal);
}
    
private Integer calculateDefaultSuggestedTime(GoalResponse goal) {
        Integer suggestedTime = 30; // Default 30 minutes
        
        // Adjust based on goal type
        if (goal.getGoalType() == GoalType.PROJECT) {
            suggestedTime = Math.max(suggestedTime, 45); // Project needs at least 45 minutes
        } else if (goal.getGoalType() == GoalType.SKILL) {
            suggestedTime = Math.max(suggestedTime, 60); // Skill needs extended session
        }
        
        return suggestedTime;
    }
    
// ... (rest of the code remains the same)
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
