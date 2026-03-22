# Architecture Decisions

This document explains key architectural decisions made during the development of the Northstar Activity Tracker, along with the reasoning behind each choice.

## 1. Why goalId on Activity is a plain Long (not @ManyToOne)

### Decision
Activity.goalId is stored as a plain Long (foreign key reference) rather than a JPA @ManyToOne relationship.

### Reasoning

**Performance Considerations**:
- Activities are high-frequency entities (many per day)
- @ManyToOne would require additional JOIN queries for every activity fetch
- Plain Long reference reduces database overhead and query complexity

**Flexibility**:
- Activities can exist independently of goals (optional linking)
- No cascade delete concerns when goals are deleted
- Allows activities to reference deleted goals without breaking referential integrity

**Simplicity**:
- Avoids lazy loading issues and N+1 query problems
- Cleaner serialization for API responses
- No need to handle detached entities in activity processing

**Trade-offs**:
- Manual validation required to ensure goalId references existing goal
- No automatic cascade operations
- Application must handle relationship integrity

### Implementation
```java
@Column(name = "goal_id")
private Long goalId; // Optional reference to Goal.id
```

## 2. Why GoalWeeklySnapshot and GoalPeriodSnapshot are separate tables

### Decision
Created two separate snapshot tables instead of a unified snapshot table.

### Reasoning

**Query Performance**:
- Weekly snapshots have specific query patterns (last 4 weeks for momentum)
- Period snapshots have different query patterns (by period type and date range)
- Separate tables allow optimized indexes for each use case

**Data Model Clarity**:
- Weekly snapshots are always 7-day periods with Monday starts
- Period snapshots support variable lengths (daily, monthly, quarterly, yearly, custom)
- Different semantics and lifecycle management

**Schema Evolution**:
- Weekly system is stable and proven
- Period system is newer (Phase 9) and may evolve
- Separation allows independent changes without affecting the other

**Storage Efficiency**:
- Weekly snapshots can use compact date representation
- Period snapshots need additional period_type column
- No wasted columns for unused features

### Alternative Considered
Single table with additional columns:
```sql
-- Rejected approach
goal_snapshots (
  id, goal_id, snapshot_type, -- WEEKLY or PERIOD
  period_start, period_end, period_type, -- Only for PERIOD
  activities_logged, consistency_score
)
```

**Why Rejected**:
- Complex queries with WHERE snapshot_type = 'WEEKLY'
- Indexes would be less efficient
- Mixed semantics in single table
- Harder to understand and maintain

## 3. Why health calculation is non-blocking (try-catch)

### Decision
Health calculation failures are caught and logged but don't prevent activity creation.

### Reasoning

**User Experience Priority**:
- Activity logging is the primary user action
- Users should never lose activity data due to health calculation issues
- Health scores can be recalculated later if needed

**System Reliability**:
- Health calculation involves complex formulas and multiple data sources
- Temporary data inconsistencies shouldn't block core functionality
- Graceful degradation allows system to continue operating

**Debugging and Monitoring**:
- Errors are logged with full context for debugging
- Manual recalculation endpoint available for fixes
- Health calculation failures are visible in logs but not to users

### Implementation
```java
@Override
public void onActivityLogged(Long goalId, LocalDate activityDate) {
    try {
        updateWeeklySnapshot(goalId, activityDate);
        updatePeriodSnapshot(goalId, activityDate);
        recalculateHealth(goalId);
    } catch (Exception e) {
        // Non-blocking — activity creation must not fail
        log.error("Failed to update health for goalId={}: {}", goalId, e.getMessage(), e);
    }
}
```

### Trade-offs
- Health scores might be temporarily inconsistent
- Requires manual intervention for some edge cases
- Additional monitoring needed to detect calculation issues

## 4. Why parent goals use rolled-up health vs own health

### Decision
Parent goals display health scores rolled up from children rather than calculating their own health.

### Reasoning

**Semantic Accuracy**:
- Parent goals represent collections of sub-goals
- Their "health" should reflect the collective health of components
- Individual parent goal tracking would be misleading

**Hierarchical Consistency**:
- Parent health always reflects current state of children
- No synchronization issues between parent and child health
- Changes in children immediately reflected in parent

**User Mental Model**:
- Users think of parent goals as "containers" for sub-goals
- "MBA is 80% healthy" means the sub-goals are averaging 80%
- Matches how people think about hierarchical objectives

**Calculation Efficiency**:
- No need to track separate activities for parent goals
- Single source of truth for parent health
- Avoids double-counting or conflicting metrics

### Rollup Formula
```java
// Priority-weighted average of child health scores
rolled_up_score = sum(child_health * child_priority_weight) / sum(priority_weights)
```

### Alternative Rejected
Calculate parent health from activities linked to parent:
- Would require activities to be linked to both parent and child goals
- Complex activity attribution rules
- Potential for double-counting

## 5. Why scheduleDays is stored as comma-separated string

### Decision
scheduleDays is stored as comma-separated string (e.g., "MON,WED,FRI") rather than normalized relationship table.

### Reasoning

**Simplicity**:
- Goals typically have small, fixed sets of schedule days
- No need for complex many-to-many relationship tables
- Easy to understand and maintain

**Performance**:
- No JOIN queries needed to fetch schedule days
- Direct string parsing in application layer
- Reduced database complexity

**Storage Efficiency**:
- Compact storage for typical use cases (2-7 days)
- No overhead of relationship tables
- Simple serialization for API responses

**Flexibility**:
- Easy to add/remove days without table modifications
- Supports any combination of days
- No constraints on day combinations

### Implementation
```java
// Stored as: "MON,WED,FRI"
@Column(name = "schedule_days")
private String scheduleDays;

// Converted to List<ScheduleDay> in getter
public List<ScheduleDay> getScheduleDaysList() {
    if (this.scheduleDays == null || this.scheduleDays.isBlank())
        return List.of();
    return Arrays.stream(this.scheduleDays.split(","))
        .map(String::trim)
        .map(ScheduleDay::valueOf)
        .collect(Collectors.toList());
}
```

### Alternative Considered
Join table approach:
```sql
-- Rejected approach
goal_schedule_days (
  goal_id, 
  schedule_day, 
  primary key (goal_id, schedule_day)
)
```

**Why Rejected**:
- Overkill for simple use case
- Additional JOIN complexity
- More database operations for simple CRUD
- Harder to serialize in API responses

## 6. Why isLeaf is computed at query time not stored

### Decision
isLeaf is calculated dynamically during queries rather than stored as a database column.

### Reasoning

**Data Consistency**:
- Leaf status changes when children are added/removed
- No risk of stale cached leaf status
- Always reflects current hierarchy state

**Storage Efficiency**:
- No additional column needed in goals table
- No need to update leaf status when hierarchy changes
- Reduces database storage requirements

**Query Performance**:
- Leaf calculation uses efficient EXISTS queries
- Can be optimized with proper database indexes
- Computation cost is minimal compared to I/O savings

### Implementation
```java
// In repository
@Query("SELECT g FROM Goal g WHERE g.userId = :userId AND g.isDeleted = false " +
       "AND NOT EXISTS (SELECT 1 FROM Goal c WHERE c.parentGoalId = g.uuid AND c.isDeleted = false)")
List<Goal> findLeafGoals(String userId);

// In service
public boolean isLeafGoal(String goalUuid, String userId) {
    return !goalRepository.existsByParentGoalIdAndUserIdAndIsDeletedFalse(goalUuid, userId);
}
```

### Alternative Rejected
Stored isLeaf column with triggers:
```sql
-- Rejected approach
ALTER TABLE goals ADD COLUMN is_leaf BOOLEAN;

CREATE TRIGGER update_is_leaf 
AFTER INSERT OR UPDATE OR DELETE ON goals
FOR EACH ROW EXECUTE FUNCTION recalculate_is_leaf();
```

**Why Rejected**:
- Complex trigger logic
- Potential for trigger bugs causing data inconsistency
- Additional maintenance overhead
- Database-specific implementation

## 7. Why evaluationPeriod WEEKLY falls back to weekly system

### Decision
When evaluationPeriod is WEEKLY, the system uses the existing weekly consistency calculation rather than creating period snapshots.

### Reasoning

**Backward Compatibility**:
- Existing weekly system is proven and stable
- No disruption to current users
- Maintains existing behavior for WEEKLY goals

**System Efficiency**:
- Avoids duplicate snapshot creation for weekly periods
- Reduces database storage and processing overhead
- Leverages existing weekly infrastructure

**Consistency**:
- Weekly evaluation periods are identical to existing weekly system
- No need for separate weekly period snapshots
- Unified approach for weekly tracking

### Implementation Logic
```java
if (goal.getEvaluationPeriod() == null
        || goal.getEvaluationPeriod() == EvaluationPeriod.WEEKLY) {
    // Use existing weekly system
    return calculateWeeklyConsistency(goal);
} else {
    // Use new period-based system
    return goal.getPeriodConsistencyScore();
}
```

### Alternative Rejected
Create period snapshots for all evaluation periods including WEEKLY:
- Would duplicate existing weekly functionality
- Additional storage overhead for no benefit
- More complex code paths for same result

## 8. Why streak uses grace period from goal priority

### Decision
Streak grace periods are derived from goal priority rather than explicit user configuration.

### Reasoning

**Simplicity**:
- Reduces configuration burden on users
- Intelligent defaults based on goal importance
- Fewer fields to manage and validate

**Semantic Alignment**:
- Higher priority goals should have less tolerance for misses
- Priority reflects goal importance, which aligns with strictness
- Natural mapping between priority level and tolerance

**Consistency**:
- Standardized approach across all goals
- Predictable behavior for users
- Easy to understand and explain

### Priority to Grace Mapping
| Priority | Weekly Misses | Monthly Misses | Rationale |
|----------|---------------|----------------|-----------|
| CRITICAL | 0             | 0              | No tolerance for critical goals |
| HIGH     | 1             | 1              | Minimal tolerance |
| MEDIUM   | 2             | 2              | Moderate tolerance |
| LOW      | 3             | 3              | High tolerance |

### Override Capability
Users can still override defaults by setting explicit `missesAllowedPerWeek` and `missesAllowedPerMonth` values.

### Alternative Rejected
User-configurable grace periods only:
- Would require additional UI complexity
- Users might set inappropriate values
- More cognitive load for setup

## 9. Option A vs Option B decision for evaluation periods

### Decision
Chose Option A (single target per evaluation period) over Option B (multiple targets per period).

### Option A (Implemented)
- Single `targetPerPeriod` field
- One target count per evaluation period
- Simple and clear semantics
- Easy to calculate consistency

### Option B (Future Enhancement)
- Multiple targets per period
- Complex target configurations
- Flexible but more complex
- Planned for future phases

### Reasoning for Option A

**Simplicity First**:
- Easier to implement and validate
- Clear user mental model
- Reduces configuration complexity
- Faster time to market

**Proven Pattern**:
- Mirrors existing weekly system (single target per week)
- Users familiar with single-target approach
- Established calculation patterns

**Performance**:
- Simpler database schema
- Faster calculations
- Less complex queries

**Future Extensibility**:
- Can add Option B later without breaking Option A
- Migration path from A to B is possible
- A serves as foundation for B

### Option B Future Implementation
Planned for future phases with features like:
```json
{
  "evaluationPeriod": "MONTHLY",
  "targets": [
    { "type": "READING", "count": 4 },
    { "type": "WRITING", "count": 8 },
    { "type": "EXERCISE", "count": 12 }
  ]
}
```

### Migration Strategy
- Phase 9: Option A (single target)
- Future Phase X: Option B (multiple targets)
- Backward compatibility maintained
- Optional upgrade path for users

### Trade-offs of Option A
- Less flexibility for complex goals
- May require multiple goals for complex tracking
- Additional goals needed for multi-target scenarios

### Benefits Outweighing Trade-offs
- Faster implementation
- Cleaner user experience initially
- Proven approach from weekly system
- Foundation for future enhancements
