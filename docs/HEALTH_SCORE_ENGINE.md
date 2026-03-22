# Health Score Engine

## Overview

The health score represents the overall well-being of a goal on a scale of 0-100, combining three key components: consistency, momentum, and progress. A higher score indicates better goal performance and health. The system provides nuanced feedback by evaluating different aspects of goal achievement, helping users understand not just whether they're hitting targets, but how consistently they're performing, whether their performance is improving or declining, and whether they're on track to complete goals on time.

The health score is calculated as a weighted combination of three components, with weights that can be customized per goal or derived from goal type defaults. This allows different types of goals to emphasize what matters most - habits might prioritize consistency, projects might prioritize progress, while fitness goals might balance all three aspects.

## Consistency Score

### Weekly Calculation (Time-Paced Formula)

For goals without evaluation period (or with WEEKLY evaluation period), consistency is calculated based on weekly targets and current progress through the week.

**Formula**:
```
consistency_score = min(100, (activities_this_week / expected_by_now) * 100)
```

**Expected by now calculation**:
```
expected_by_now = target_frequency_weekly * (day_of_week / 7.0)
```

Where day_of_week is 1-7 (Monday=1, Sunday=7).

**What makes it null (untracked)**:
- `targetFrequencyWeekly` is null or 0
- No weekly snapshot exists for current week

### Period-Based Calculation (When EvaluationPeriod is Set)

For goals with non-weekly evaluation periods, consistency uses the period-based score stored on the goal.

**Logic**:
1. If `evaluationPeriod` is set and not WEEKLY, use `periodConsistencyScore`
2. If `periodConsistencyScore` is null, return null (untracked)
3. Otherwise, use the stored period consistency score

**Period consistency calculation** (in PeriodUtils):
```
period_score = min(100, (activities_logged / target_per_period) * 100)
```

### Example Walkthrough

**Scenario**: Target 5x/week, today is Wednesday, logged 2 times

**Calculation**:
- `target_frequency_weekly` = 5
- `day_of_week` = 3 (Wednesday)
- `expected_by_now` = 5 * (3/7.0) = 2.14
- `activities_this_week` = 2
- `consistency_score` = min(100, (2 / 2.14) * 100) = 93.5%

**Interpretation**: 93.5% consistency - slightly behind expected pace but still very good.

## Momentum Score

### Rolling 4-Week Weighted Average Formula

Momentum measures the trend of goal performance over the last 4 weeks using a weighted average that gives more importance to recent performance.

**Weights**:
- Week 3 (oldest): 10%
- Week 2: 20%
- Week 1: 30%
- Current week: 40%

**Formula**:
```
momentum_score = (week3_score * 0.10) + 
                (week2_score * 0.20) + 
                (week1_score * 0.30) + 
                (current_week_score * 0.40)
```

**What makes it null (untracked)**:
- `targetFrequencyWeekly` is null or 0
- Fewer than 2 weeks of data available

### Streak Multiplier Table

Momentum is boosted by streak performance:

| Current Streak | Multiplier | Effect |
|----------------|------------|--------|
| 0              | 0.8        | 20% penalty |
| 1-2            | 0.9        | 10% penalty |
| 3-4            | 1.0        | No effect |
| 5-7            | 1.1        | 10% bonus |
| 8+             | 1.2        | 20% bonus |

**Final momentum with streak**:
```
final_momentum = momentum_score * streak_multiplier
```

### Example Walkthrough

**Scenario**: Last 4 weeks performance for 5x/week goal
- Week 3: 3/5 activities (60%)
- Week 2: 4/5 activities (80%)
- Week 1: 5/5 activities (100%)
- Current: 4/5 activities so far (80%)
- Current streak: 6 weeks

**Calculation**:
1. **Weighted average**: (60% * 0.10) + (80% * 0.20) + (100% * 0.30) + (80% * 0.40) = 82%
2. **Streak multiplier**: 6 weeks streak → 1.1 multiplier
3. **Final momentum**: 82% * 1.1 = 90.2%

**Interpretation**: Strong momentum with recent consistency and streak bonus.

## Progress Score (Time-Paced)

### Formula: Actual vs Expected Progress

Progress score measures whether you're ahead or behind schedule based on time elapsed.

**Formula**:
```
progress_score = min(100, (actual_progress / expected_progress) * 100)
```

**Expected progress calculation**:
```
expected_progress = (days_elapsed / total_days) * 100
```

**Why it's better than raw progress percentage**:
Raw progress doesn't account for time. 30% complete could be excellent if you're only 20% through the timeline, but poor if you're 80% through.

### Example Calculations

**Example 1: Ahead of Schedule**
- Progress: 30% done
- Time: 20% through timeline
- `progress_score` = min(100, (30% / 20%) * 100) = 100%
- **Interpretation**: Ahead of schedule, capped at 100%

**Example 2: Behind Schedule**
- Progress: 30% done  
- Time: 80% through timeline
- `progress_score` = min(100, (30% / 80%) * 100) = 37.5%
- **Interpretation**: Behind schedule, needs acceleration

### Fallback When No Dates Are Set

If `startDate` or `targetDate` are not set:
- Progress score falls back to raw `progressPercentage`
- No time-based adjustment applied
- Score is simply the raw progress percentage

## Final Health Score

### Weighted Combination Formula

The final health score combines all three components using goal-specific weights:

**Formula**:
```
health_score = (consistency_score * consistency_weight/100) +
              (momentum_score * momentum_weight/100) +
              (progress_score * progress_weight/100)
```

### How Effective Weights Are Resolved

Weights are resolved in this priority order:

1. **Explicit weights** (if set on goal)
   - `consistencyWeight`, `momentumWeight`, `progressWeight`
   - Must sum to 100 when all three are set

2. **GoalType defaults** (if explicit weights not set)
   - HABIT: 60% consistency, 30% momentum, 10% progress
   - PROJECT: 20% consistency, 20% momentum, 60% progress
   - SKILL: 40% consistency, 30% momentum, 30% progress
   - FITNESS: 50% consistency, 40% momentum, 10% progress
   - GENERAL: 34% consistency, 33% momentum, 33% progress

3. **GENERAL defaults** (if goalType not set)
   - 34% consistency, 33% momentum, 33% progress

### Example with FITNESS Goal

**Scenario**: FITNESS goal with default weights
- Consistency score: 85%
- Momentum score: 90%
- Progress score: 70%
- FITNESS weights: 50% consistency, 40% momentum, 10% progress

**Calculation**:
```
health_score = (85% * 0.50) + (90% * 0.40) + (70% * 0.10)
             = 42.5% + 36% + 7%
             = 85.5%
```

**Interpretation**: Strong overall health, with consistency being the most important factor.

## Health Status Thresholds

| Score Range | Status | Meaning | Color (UI) |
|-------------|--------|---------|-------------|
| 80-100      | THRIVING | Excellent performance, on track | Green |
| 60-79       | ON_TRACK | Good performance, minor issues | Blue |
| 40-59       | AT_RISK  | Needs attention, declining | Yellow |
| 0-39        | CRITICAL | Poor performance, urgent action needed | Red |
| null        | UNTRACKED | No tracking configured | Gray |

## Streak System

### How Streak Is Counted

Streak counts consecutive successful periods (weeks or evaluation periods):

1. **Weekly streak**: Consecutive weeks meeting or exceeding target
2. **Period streak**: Consecutive evaluation periods meeting target (Phase 9)
3. **Success criteria**: Activities >= target for the period

### How Grace Period Affects Streak

Streak continues as long as misses are within allowed limits:

- **Weekly grace**: `missesAllowedPerWeek` (priority-based defaults)
- **Monthly grace**: `missesAllowedPerMonth` (priority-based defaults)
- **Streak breaks**: When exceeds grace limits OR zero activities

### Longest Streak Purpose

- **Motivation**: Shows historical best performance
- **Benchmark**: Gives users a target to beat
- **Never decreases**: Preserves best achievement even after streak breaks

## Weekly Snapshot System

### What GoalWeeklySnapshot Stores

- `goal_id`: Reference to the goal
- `week_start`: Monday of the week (unique per goal per week)
- `activities_logged`: Number of activities logged that week
- `target_frequency_weekly`: Target for that week (snapshot)
- `consistency_score_for_week`: Consistency score for that week

### When It Is Created/Updated

**Created**:
- When first activity logged for a goal in a new week
- Automatically in `updateWeeklySnapshot()` method

**Updated**:
- Every time an activity is logged for that goal in the current week
- Increments `activities_logged` and recalculates `consistency_score_for_week`

### How It Feeds Momentum Calculation

- Momentum uses last 4 weeks of snapshots
- Each week's `consistency_score_for_week` is used in weighted average
- Missing weeks are treated as 0% consistency

### Weekly Reset Job Behavior

**Runs**: Every Monday at 00:05 AM
**Job**: `WeeklySnapshotResetJob`
**Actions**:
- Recalculates health scores for all goals
- Updates streaks based on previous week performance
- Creates new week snapshots as needed
- Handles period transitions for evaluation periods

## Period Snapshot System (Phase 9)

### What GoalPeriodSnapshot Stores

- `goal_id`: Reference to the goal
- `period_type`: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
- `period_start`: Start date of the period
- `period_end`: End date of the period (inclusive)
- `activities_logged`: Activities in this period
- `target_per_period`: Target for this period (snapshot)
- `consistency_score`: Period consistency score

### How It Differs From Weekly Snapshots

| Aspect | Weekly Snapshots | Period Snapshots |
|--------|------------------|------------------|
| Purpose | Momentum calculation | Alternative consistency tracking |
| Frequency | Always weekly | Configurable (daily, monthly, etc.) |
| Usage | Momentum score only | Consistency score when evaluationPeriod set |
| Coexistence | Always created | Created only when evaluationPeriod ≠ WEEKLY |

### When It Is Created/Updated

**Created**:
- When first activity logged for a goal in a new evaluation period
- In `updatePeriodSnapshot()` method

**Updated**:
- Every time an activity is logged for that goal in the current period
- Increments `activities_logged` and recalculates `consistency_score`

### How Period Consistency Score Is Stored on Goal

- `periodConsistencyScore` field stores current period's consistency
- Updated each time activity is logged
- Used by `calculateConsistencyScore()` when evaluationPeriod is set
- Provides real-time consistency feedback

## Rollup Engine (Parent Goals)

### Priority Weights Table

| Priority | Weight | Description |
|----------|--------|-------------|
| CRITICAL | 4      | Highest importance in rollup |
| HIGH     | 3      | High importance |
| MEDIUM   | 2      | Medium importance |
| LOW      | 1      | Lowest importance |

### How Rolled-Up Health Score Is Calculated

**Formula**:
```
rolled_up_score = sum(child_health_score * child_priority_weight) / sum(child_priority_weights)
```

**Steps**:
1. Get all non-deleted child goals
2. For each child, get health score and priority weight
3. Multiply health score by priority weight
4. Sum all weighted scores
5. Divide by sum of all priority weights
6. Cap at 100

### ParentInsights Block Contents

The `ParentInsights` block provides comprehensive parent goal statistics:

```json
{
  "totalChildren": 5,
  "thrivingChildren": 2,
  "onTrackChildren": 1,
  "atRiskChildren": 1,
  "criticalChildren": 1,
  "untrackedChildren": 0,
  "childrenSummary": {
    "THRIVING": 2,
    "ON_TRACK": 1,
    "AT_RISK": 1,
    "CRITICAL": 1,
    "UNTRACKED": 0
  },
  "weakestChild": {
    "uuid": "child-uuid",
    "title": "Child Goal Title",
    "healthScore": 25.0
  },
  "completionVelocity": {
    "overallProgress": 65.0,
    "averageHealth": 72.5,
    "trendDirection": "IMPROVING"
  },
  "healthScoreLastWeek": 68.0
}
```

### WeakestChild Identification

- Child with lowest health score
- Used to highlight which child needs most attention
- Excludes UNTRACKED children (null health scores)

### CompletionVelocity Logic

**Components**:
- `overallProgress`: Average progress percentage of children
- `averageHealth`: Average health score of tracked children
- `trendDirection`: Based on current vs last week health score

**Trend calculation**:
- `IMPROVING`: current > lastWeek + 3 points
- `DECLINING`: current < lastWeek - 3 points  
- `STABLE`: within ±3 points

### HealthScoreLastWeek Calculation

- Stored on parent goal during weekly rollup
- Used for trend analysis in ParentInsights
- Calculated during weekly health recalculation

## When Health Is Recalculated

### On Activity Log (Automatic)

**Trigger**: Every time an activity is created
**Method**: `GoalHealthServiceV1.onActivityLogged()`
**Actions**:
1. Updates weekly snapshot for consistency
2. Updates period snapshot (if evaluationPeriod set)
3. Recalculates health score for the specific goal
4. Non-blocking - activity creation succeeds even if health calculation fails

### On Manual Recalculate Endpoint

**Endpoint**: `PATCH /api/v1/goals/{id}/health/recalculate`
**Method**: `GoalHealthServiceV1.recalculateHealth()`
**Actions**:
1. Recalculates health for specific goal
2. Updates parent goals (rollup)
3. Returns updated goal response
4. Used for manual health fixes or debugging

### On Weekly Scheduled Job

**Schedule**: Every Monday at 00:05 AM
**Job**: `WeeklySnapshotResetJob`
**Actions**:
1. Recalculates health for all goals
2. Updates streaks based on previous week
3. Handles period transitions for evaluation periods
4. Ensures data consistency across the system
5. Creates new weekly snapshots for the current week
