# Smart Todo List

## Overview

The Smart Todo List is an intelligent daily task generation system that answers "What should I do TODAY?" by providing a prioritized, schedule-aware todo list based on user goals. Unlike traditional todo lists that require manual entry, the Smart Todo List automatically generates daily tasks from existing goals, considering schedule patterns, priority levels, progress tracking, and urgency calculations.

The system provides a dashboard widget that presents today's actionable items in a smart, context-rich format that can be integrated with frontend activity logging.

## Architecture

### Components

**Service Layer:**
- `SmartTodoService` - Interface defining the contract for smart todo operations
- `SmartTodoServiceV1` - Implementation with intelligent daily task generation logic

**Controller Layer:**
- `TodoController` - REST endpoints for smart todo operations

**DTOs:**
- `SmartTodoResponse` - Rich response DTO with goal context, progress, priority, schedule info, streaks, and urgency

### API Endpoints

```
GET    /api/v1/todos/today                - Get today's smart todo list
POST   /api/v1/todos/refresh              - Refresh today's todo list
```

## Intelligent Task Generation

### Schedule Awareness

The system considers goal schedules to determine if a task should appear today:

**Schedule Types:**
- `FLEXIBLE` - Any day (default for habits and general goals)
- `SPECIFIC_DAYS` - Configured specific days (e.g., MON, WED, FRI)

**Schedule Logic:**
- For FLEXIBLE goals: Always appear in today's todo list
- For SPECIFIC_DAYS goals: Only appear if today matches configured schedule days

### Priority-Based Sorting

Todo items are sorted using a multi-level priority system:

**Primary Sort (Goal Priority):**
- P1 (CRITICAL) - Highest priority goals
- P2 (HIGH) - High priority goals
- P3 (MEDIUM) - Medium priority goals
- P4 (LOW) - Low priority goals

**Secondary Sort (within same priority):**
1. Streak at risk (goals that will break a streak if not completed today)
2. Behind schedule (goals falling behind their planned pace)
3. Scheduled for today (vs. not scheduled)
4. Incomplete first (vs. already completed today)

### Progress Tracking

Each todo item displays progress information:

**Progress Metrics:**
- `currentProgress` - Number of activities completed today
- `targetProgress` - Target number of activities for today
- `progressPercentage` - Completion percentage (current/target * 100)
- `isCompletedToday` - Boolean indicating if target is met

**Progress Calculation:**
- FLEXIBLE goals: Target based on weekly frequency divided by 7
- SPECIFIC_DAYS goals: Target = 1 on scheduled days, 0 otherwise

### Streak and Urgency

The system identifies urgent tasks that need immediate attention:

**Streak At Risk:**
- Daily goals with active streaks that were missed yesterday
- Displayed with urgency reason: "Streak breaks today - complete daily goal!"

**Behind Schedule:**
- Goals falling behind their planned completion pace
- Displayed with urgency reason: "Weekly session due today"

### Time Suggestions

The system provides intelligent time estimates for task completion:

**Base Time:**
- Uses `minimumSessionMinutes` from goal configuration (default: 30 minutes)

**Goal Type Adjustments:**
- PROJECT goals: Minimum 45 minutes (complex tasks need more time)
- SKILL goals: Minimum 60 minutes (skill development needs extended sessions)


## SmartTodoResponse DTO

The response DTO provides rich context for each todo item:

**Basic Information:**
- `goalId` - Goal identifier
- `title` - Goal title
- `description` - Goal description
- `priority` - Goal priority enum
- `goalType` - Goal type enum
- `priorityDisplay` - Human-readable priority (P1, P2, P3, P4)

**Schedule Information:**
- `scheduledForToday` - Boolean indicating if task is scheduled today
- `scheduleType` - Schedule type (FLEXIBLE, SPECIFIC_DAYS)
- `scheduleDetails` - Human-readable schedule details

**Progress Tracking:**
- `currentProgress` - Current progress count
- `targetProgress` - Target progress count
- `progressPercentage` - Completion percentage
- `isCompletedToday` - Completion status

**Streak and Urgency:**
- `currentStreak` - Current streak count
- `streakAtRisk` - Boolean indicating if streak is at risk
- `isBehindSchedule` - Boolean indicating if behind schedule
- `urgencyReason` - Human-readable urgency reason

**Time Suggestions:**
- `minimumSessionMinutes` - Minimum session time
- `suggestedTimeMinutes` - Suggested time for this session
- `lastCompletedDate` - Last activity completion date

**Logging Context:**
- `requiresQuickLog` - Boolean indicating quick log availability
- `quickLogContext` - Pre-filled context for logging

## Implementation Details

### SmartTodoServiceV1

**Key Methods:**

- `getTodaySmartTodos(String userId)` - Generates today's smart todo list
- `refreshTodayTodos(String userId)` - Refreshes todo list with updated data

**Helper Methods:**

- `setScheduleInfo()` - Sets schedule-aware information
- `setProgressInfo()` - Sets progress tracking metrics
- `setStreakAndUrgency()` - Sets streak and urgency indicators
- `setTimeSuggestions()` - Sets intelligent time estimates
- `sortSmartTodos()` - Applies multi-level priority sorting
- `parseScheduleDays()` - Parses schedule day list to set
- `getTargetForToday()` - Calculates target progress for today
- `getLastActivityDate()` - Retrieves last activity date
- `getScheduleDisplay()` - Generates human-readable schedule display

### Current Limitations

**Progress Tracking:**
- Currently uses simulated random progress for demonstration
- Requires integration with Activity service for real progress data
- `getTodayActivitiesForGoal()` method needs implementation

**Activity Logging:**
- Quick complete currently logs completion without creating Activity record
- Requires integration with Activity service for actual activity creation
- `getLastActivityDate()` returns empty string (needs implementation)

**Historical Data:**
- Streak at risk detection uses random simulation (20% chance)
- Behind schedule detection uses random simulation (30% chance)
- Requires actual historical activity data for accurate calculations

## Future Enhancements

**Real-Time Progress:**
- Integrate with Activity service to fetch actual today's progress
- Calculate progress based on real activity logs
- Provide accurate streak and urgency detection

**Advanced Scheduling:**
- Support for WEEKLY and MONTHLY schedule types
- Complex schedule patterns (e.g., "first Monday of each month")
- Schedule conflicts resolution

**Personalization:**
- Learn user's typical completion times
- Adjust time suggestions based on historical data
- Personalized priority weighting

**Analytics:**
- Daily completion rate tracking
- Streak analytics and insights
- Schedule adherence reporting

## Integration Points

**GoalService:**
- Fetches user's goals via `getAllGoalsByUser()`
- Fetches individual goal details via `getGoalById()`

**ActivityService:**
- Creates activity records from quick complete (pending integration)
- Fetches today's activities for progress calculation (pending integration)

**GoalHealthService:**
- Potential integration for health score-based priority adjustment
- Health trend analysis for urgency calculation

## Testing

Recommended test scenarios:

1. **Schedule Types:**
   - FLEXIBLE goals appearing every day
   - SPECIFIC_DAYS goals appearing only on configured days

2. **Priority Sorting:**
   - P1 goals appearing before P2 goals
   - Streak at risk goals prioritized within same priority
   - Behind schedule goals prioritized within same priority

3. **Progress Tracking:**
   - Accurate progress calculation for different schedule types
   - Completion status updates correctly

4. **Quick Complete:**
   - Quick complete creates activity record
   - Todo list refreshes with updated progress
   - Pre-filled context is correct

5. **Time Suggestions:**
   - Appropriate time estimates for different goal types
   - Minimum session time respected
