# Goal System

## Goal Hierarchy

### How Parent-Child Relationships Work

Goals are organized in hierarchical trees where parent goals represent broader objectives and child goals represent specific tasks or sub-goals. The hierarchy uses UUID-based references through the `parent_goal_id` field, which stores the UUID of the parent goal (not the numeric ID).

### How parentGoalId Links Goals

- `parent_goal_id` field stores the UUID of the parent goal
- Root goals have `parent_goal_id` as null
- Child goals reference their parent's UUID
- The hierarchy can be multiple levels deep (e.g., MBA → CAT Prep → Quantitative → Daily Problems)

### Leaf vs Parent Node Concept

- **Leaf Goals**: Goals with no children (actual trackable activities)
  - Have tracking configuration (targetFrequencyWeekly, targetPerPeriod, etc.)
  - Health scores calculated from actual activity data
  - Display tracking progress and streak information
- **Parent Goals**: Goals with children (summary/rollup goals)
  - Health scores rolled up from children using priority weights
  - Display `parentInsights` block with child statistics
  - No direct tracking configuration (inherited from children)

### Example Tree

```
Complete MBA (Parent Goal)
├── CAT Preparation (Parent Goal)
│   ├── Quantitative Practice (Leaf Goal)
│   │   ├── Daily Problems (Leaf Goal)
│   │   └── Weekly Mock Tests (Leaf Goal)
│   ├── Verbal Practice (Leaf Goal)
│   └── Reading Comprehension (Leaf Goal)
└── Networking Events (Leaf Goal)
```

## Goal Types and Default Weights

Each GoalType has default weight distributions for the three health score components:

| GoalType | Consistency Weight | Momentum Weight | Progress Weight | Description |
|----------|-------------------|-----------------|-----------------|-------------|
| HABIT    | 60%               | 30%             | 10%             | Daily routines, fitness habits |
| PROJECT  | 20%               | 20%             | 60%             | Project-based goals with clear milestones |
| SKILL    | 40%               | 30%             | 30%             | Skill development, learning goals |
| FITNESS  | 50%               | 40%             | 10%             | Physical fitness, exercise goals |
| GENERAL  | 34%               | 33%             | 33%             | Default for uncategorized goals |

### When to Use Each Type

- **HABIT**: Daily routines, meditation, exercise, reading habits
- **PROJECT**: Software projects, home renovations, business launches
- **SKILL**: Learning languages, musical instruments, professional certifications
- **FITNESS**: Weight loss, strength training, marathon preparation
- **GENERAL**: Multi-faceted goals that don't fit other categories

## Schedule Configuration

### FLEXIBLE vs SPECIFIC_DAYS

**FLEXIBLE**: User can log activities any day of the week
- Default for PROJECT, SKILL, GENERAL goal types
- No specific day requirements
- Good for irregular schedules

**SPECIFIC_DAYS**: Activities only count on designated days
- Default for HABIT, FITNESS goal types
- Uses `scheduleDays` field with comma-separated values (e.g., "MON,WED,FRI")
- Prevents double logging on non-scheduled days

### Schedule Days Format

Stored as comma-separated string in database:
- `"MON,WED,FRI"` - Monday, Wednesday, Friday
- `"TUE,THU"` - Tuesday, Thursday  
- `"MON,TUE,WED,THU,FRI,SAT,SUN"` - Every day

### Minimum Session Minutes

- Optional filter for activity counting
- Activities shorter than this duration don't count toward goal
- `null` means any duration counts
- Useful for distinguishing meaningful vs token activities

### Allow Double Logging

- Controls whether multiple activities can be logged for same goal on same day
- `true` (default): Multiple activities allowed
- `false`: Only one activity per day per goal
- Prevents gaming the system with multiple small activities

### GoalType Schedule Defaults

| GoalType | Default Schedule Type | Typical Use Case |
|----------|---------------------|------------------|
| HABIT    | SPECIFIC_DAYS        | Daily routines with fixed schedule |
| FITNESS  | SPECIFIC_DAYS        | Workout schedules |
| PROJECT  | FLEXIBLE             | Irregular project work |
| SKILL    | FLEXIBLE             | Practice sessions when available |
| GENERAL  | FLEXIBLE             | Mixed approach goals |

## Grace Period

### Misses Allowed Per Week/Month

Users can specify how many misses are permitted before streak is broken:

- `missesAllowedPerWeek`: Number of missed weeks allowed
- `missesAllowedPerMonth`: Number of missed months allowed
- If null, falls back to priority-based defaults

### Priority-Based Defaults Table

| Priority | Weekly Misses Allowed | Monthly Misses Allowed | Rationale |
|----------|----------------------|-----------------------|-----------|
| CRITICAL | 0                    | 0                     | No tolerance for misses |
| HIGH     | 1                    | 1                     | Minimal tolerance |
| MEDIUM   | 2                    | 2                     | Moderate tolerance |
| LOW      | 3                    | 3                     | High tolerance |

### How It Affects Streak Calculation

Streak continues as long as:
1. Activities meet or exceed target within grace period
2. Weekly/monthly misses don't exceed allowed limits
3. Once limits exceeded, streak resets to 0

Example: HIGH priority goal with 1 miss allowed per week
- Week 1: 5/5 activities (streak: 1)
- Week 2: 4/5 activities (streak: 2, within grace)
- Week 3: 3/5 activities (streak: 3, within grace)
- Week 4: 2/5 activities (streak: 4, within grace)
- Week 5: 1/5 activities (streak: 5, within grace)
- Week 6: 0/5 activities (streak: 6, within grace)
- Week 7: 0/5 activities (streak: 7, grace exceeded) → streak resets

## Evaluation Period (Phase 9)

### All EvaluationPeriod Types with Examples

| Period | Description | Example Target | Use Case |
|--------|-------------|----------------|----------|
| DAILY   | Target per day | 2 problems/day | Daily practice habits |
| WEEKLY  | Target per week | 3 sessions/week | Traditional weekly goals |
| MONTHLY | Target per month | 4 books/month | Monthly reading goals |
| QUARTERLY | Target per 3 months | 90 sessions/quarter | Long-term fitness goals |
| YEARLY  | Target per year | 12 projects/year | Annual objectives |
| CUSTOM  | Target per N days | 5 problems/10 days | Irregular cycles |

### Target Per Period

- `targetPerPeriod`: Number of activities expected in one evaluation period
- Required when `evaluationPeriod` is set
- Used for consistency score calculation
- Example: Monthly goal with `targetPerPeriod: 4` means 4 activities per month

### Custom Period Days

- `customPeriodDays`: Number of days in a CUSTOM evaluation period
- Required only when `evaluationPeriod = CUSTOM`
- Defines the length of custom cycles
- Example: `customPeriodDays: 10` means 10-day cycles

### How EvaluationPeriod Interacts with Weekly System

The evaluation period system runs **alongside** the existing weekly system:

1. **If evaluationPeriod is null or WEEKLY**: Uses existing weekly system
   - Consistency calculated from weekly snapshots
   - Momentum from 4-week rolling average
   - Progress from time-paced calculation

2. **If evaluationPeriod is non-weekly**: Uses period-based consistency
   - Consistency calculated from period snapshots
   - Momentum still uses weekly system (unchanged)
   - Progress still uses time-paced calculation (unchanged)

3. **Both systems operate independently**:
   - Weekly snapshots continue to be created for momentum
   - Period snapshots created for alternative consistency scoring
   - Health score combines period consistency + weekly momentum + progress

### Example Comparison

**Reading Goal (MONTHLY)**:
```json
{
  "title": "Reading Challenge",
  "evaluationPeriod": "MONTHLY",
  "targetPerPeriod": 4,
  "goalType": "GENERAL"
}
```
- Consistency: 4 books per month target
- Momentum: Still calculated from weekly reading patterns
- Progress: Based on annual reading target vs time elapsed

**Quantitative Practice (DAILY)**:
```json
{
  "title": "Daily Quantitative",
  "evaluationPeriod": "DAILY", 
  "targetPerPeriod": 2,
  "goalType": "SKILL"
}
```
- Consistency: 2 problems per day target
- Momentum: Weekly patterns of daily practice
- Progress: Total problems solved vs target

## Goal Status Lifecycle

### All Statuses and Transitions

| Status | Description | When Auto-Changes |
|--------|-------------|-------------------|
| NOT_STARTED | Goal created but no activity yet | First activity logged → IN_PROGRESS |
| IN_PROGRESS | Active goal with activities | Target achieved → COMPLETED<br>Target date passed → OVERDUE |
| COMPLETED | Goal target achieved | Manual change back to IN_PROGRESS (rare) |
| OVERDUE | Target date passed without completion | Manual change back to IN_PROGRESS (if still pursuing) |

### When Status Auto-Changes

**NOT_STARTED → IN_PROGRESS**:
- Triggered when first activity is logged for the goal
- Happens automatically in ActivityServiceV1 after activity creation

**IN_PROGRESS → COMPLETED**:
- Triggered when `currentValue >= targetValue` (for GREATER_THAN)
- Triggered when `currentValue <= targetValue` (for LESS_THAN)
- Triggered when `currentValue == targetValue` (for EQUAL)
- Happens during health recalculation

**IN_PROGRESS → OVERDUE**:
- Triggered when `targetDate` is in the past AND status is still IN_PROGRESS
- Checked during health recalculation
- Does not auto-change if already COMPLETED

**Manual Changes**:
- Users can manually change status through API
- System respects manual changes and recalculates accordingly
- COMPLETED goals can be reactivated if needed
