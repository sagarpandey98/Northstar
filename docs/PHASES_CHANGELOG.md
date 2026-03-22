# Phases Changelog

This document tracks the evolution of the Northstar Activity Tracker through development phases, showing how features were added incrementally.

## Phase 1: Foundation System

**Objective**: Establish core goal tracking with health scoring engine

### Features Added

#### Goal Entity Enhancements
- **GoalType enum**: HABIT, PROJECT, SKILL, FITNESS, GENERAL
- **Target Configuration**:
  - `targetFrequencyWeekly` - Activities per week target
  - `targetVolumeDaily` - Daily volume target
- **Schedule System**:
  - `scheduleType` - FLEXIBLE vs SPECIFIC_DAYS
  - `scheduleDays` - Comma-separated days (e.g., "MON,WED,FRI")
  - `minimumSessionMinutes` - Minimum duration for counting
  - `allowDoubleLogging` - Multiple activities per day control
- **Grace Period**:
  - `missesAllowedPerWeek` / `missesAllowedPerMonth`
  - Priority-based defaults (CRITICAL=0, HIGH=1, MEDIUM=2, LOW=3)
- **Health Score Weights**:
  - `consistencyWeight`, `momentumWeight`, `progressWeight`
  - GoalType-based default weights
- **Calculated Fields**:
  - `consistencyScore`, `momentumScore`, `healthScore`
  - `healthStatus` (THRIVING, ON_TRACK, AT_RISK, CRITICAL, UNTRACKED)
  - `currentStreak`, `longestStreak`

#### Health Score Engine
- **Three-Component System**: Consistency (60-20-40%), Momentum (30-20-40%), Progress (10-60-10%)
- **Time-Paced Consistency**: Expected progress based on day of week
- **4-Week Momentum**: Weighted rolling average (10%-20%-30%-40%)
- **Progress Scoring**: Actual vs expected progress based on timeline
- **Streak System**: Grace period integration with priority-based tolerance

#### Weekly Snapshot System
- **GoalWeeklySnapshot Entity**: Track activities per week
- **Automatic Creation**: Created on first activity in new week
- **Momentum Data Source**: Feeds 4-week momentum calculation
- **Weekly Reset Job**: Every Monday 00:05 AM

#### API Enhancements
- **Goal CRUD**: Full create, read, update, delete operations
- **Statistics Endpoint**: Goal completion and distribution metrics
- **Category System**: Domain → Subdomain → Specific hierarchy
- **Activity System**: Activity logging with optional goal linking

### Database Schema Changes
```sql
-- Goals table additions
ALTER TABLE goals ADD COLUMN goal_type VARCHAR(50);
ALTER TABLE goals ADD COLUMN target_frequency_weekly INTEGER;
ALTER TABLE goals ADD COLUMN target_volume_daily INTEGER;
ALTER TABLE goals ADD COLUMN schedule_type VARCHAR(50);
ALTER TABLE goals ADD COLUMN schedule_days VARCHAR(100);
ALTER TABLE goals ADD COLUMN minimum_session_minutes INTEGER;
ALTER TABLE goals ADD COLUMN allow_double_logging BOOLEAN;
ALTER TABLE goals ADD COLUMN misses_allowed_per_week INTEGER;
ALTER TABLE goals ADD COLUMN misses_allowed_per_month INTEGER;
ALTER TABLE goals ADD COLUMN consistency_weight INTEGER;
ALTER TABLE goals ADD COLUMN momentum_weight INTEGER;
ALTER TABLE goals ADD COLUMN progress_weight INTEGER;
ALTER TABLE goals ADD COLUMN consistency_score DOUBLE PRECISION;
ALTER TABLE goals ADD COLUMN momentum_score DOUBLE PRECISION;
ALTER TABLE goals ADD COLUMN health_score DOUBLE PRECISION;
ALTER TABLE goals ADD COLUMN health_status VARCHAR(50);
ALTER TABLE goals ADD COLUMN current_streak INTEGER;
ALTER TABLE goals ADD COLUMN longest_streak INTEGER;

-- Weekly snapshots table
CREATE TABLE goal_weekly_snapshots (
    id BIGINT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    week_start DATE NOT NULL,
    activities_logged INTEGER NOT NULL DEFAULT 0,
    target_frequency_weekly INTEGER,
    consistency_score_for_week DOUBLE PRECISION,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (goal_id, week_start)
);
```

### Key Decisions
- Manual getters/setters (no Lombok) for entities
- Time-paced consistency calculation
- Priority-based grace period defaults
- Non-blocking health calculation (try-catch)

---

## Phase 2: Goal Hierarchy and Rollup

**Objective**: Implement parent-child goal relationships with health rollup

### Features Added

#### Goal Hierarchy
- **parent_goal_id**: UUID-based parent references
- **Leaf vs Parent Logic**: Computed at query time
- **Hierarchical Trees**: Multi-level goal structures

#### Rollup Engine
- **Priority-Weighted Rollup**: Parent health from children
- **Priority Weights**: CRITICAL=4, HIGH=3, MEDIUM=2, LOW=1
- **ParentInsights Block**: Comprehensive parent statistics
- **WeakestChild Identification**: Lowest health score child
- **CompletionVelocity**: Progress and health trends

#### API Enhancements
- **Tree Endpoint**: `/api/v1/goals/tree` - Hierarchical goal structures
- **Parent/Child Filters**: Separate endpoints for leaf vs parent goals
- **Subtree Operations**: Query specific goal subtrees

### ParentInsights Structure
```json
{
  "totalChildren": 5,
  "thrivingChildren": 2,
  "onTrackChildren": 1,
  "atRiskChildren": 1,
  "criticalChildren": 1,
  "untrackedChildren": 0,
  "childrenSummary": {
    "THRIVING": 2, "ON_TRACK": 1, "AT_RISK": 1, "CRITICAL": 1, "UNTRACKED": 0
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

### Key Decisions
- UUID-based parent references (not numeric IDs)
- Computed isLeaf field (not stored)
- Priority-weighted health rollup
- Comprehensive parent insights

---

## Phase 3: ResponseWrapper Standardization

**Objective**: Standardize API response format across all endpoints

### Features Added

#### ResponseWrapper DTO
- **Standardized Format**: success, message, data structure
- **Error Handling**: Consistent error response format
- **Validation Errors**: Detailed field-level error messages

#### API Response Standardization
- **All Endpoints**: Wrapped in ResponseWrapper
- **Error Responses**: Standardized error codes and messages
- **Validation Messages**: Field-specific validation feedback

### Response Structure
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { /* actual response data */ }
}
```

### Error Structure
```json
{
  "success": false,
  "message": "Validation failed",
  "error": "VALIDATION_ERROR",
  "details": {
    "title": "Goal title is required",
    "priority": "Goal priority is required"
  }
}
```

### Key Decisions
- Consistent response format across all APIs
- Detailed validation error messages
- Backward compatibility maintained

---

## Phase 4: Enhanced Activity System

**Objective**: Improve activity tracking with goal linking and search capabilities

### Features Added

#### Activity-Goal Linking
- **goalId Field**: Optional Long reference to Goal.id
- **Health Integration**: Activities trigger health recalculation
- **Automatic Updates**: Health scores updated on activity creation

#### Activity Search
- **Full-text Search**: Search by name and description
- **Category Filtering**: Filter by domain, subdomain, specific
- **Date Range Filtering**: Search within time periods

#### Bulk Operations
- **Bulk Create**: Create multiple activities in single request
- **Bulk Response**: Detailed success/failure reporting

### API Enhancements
- **Search Endpoint**: `/api/v1/activities/search` with filters
- **Bulk Create**: `/api/v1/activities/bulk` endpoint
- **Goal Linking**: Optional goalId in activity creation

### Key Decisions
- Optional goal linking (activities can exist independently)
- Plain Long goalId (not @ManyToOne relationship)
- Comprehensive search and filtering capabilities

---

## Phase 5: Scheduled Jobs and Background Processing

**Objective**: Implement automated health recalculation and data maintenance

### Features Added

#### Weekly Snapshot Reset Job
- **Schedule**: Every Monday 00:05 AM
- **Health Recalculation**: All goals health scores updated
- **Streak Updates**: Based on previous week performance
- **New Week Setup**: Create weekly snapshots for current week

#### Background Processing
- **Non-blocking Health**: Health failures don't block activity creation
- **Error Logging**: Comprehensive error logging for debugging
- **Manual Recalculation**: Admin endpoint for manual health fixes

### Job Configuration
```java
@Scheduled(cron = "0 5 0 * * MON") // Monday 00:05 AM
public void resetWeeklySnapshots() {
    // Recalculate health for all goals
    // Update streaks based on previous week
    // Create new week snapshots
}
```

### Key Decisions
- Non-blocking health calculation
- Comprehensive error logging
- Manual override capabilities

---

## Phase 6: Enhanced Validation and Error Handling

**Objective**: Improve input validation and error handling throughout the system

### Features Added

#### Enhanced Validation
- **Goal Weight Validator**: Ensure health weights sum to 100
- **Business Rule Validation**: Goal hierarchy validation
- **Date Validation**: Start date before target date
- **Progress Validation**: Non-negative progress values

#### Custom Exceptions
- **ValidationException**: Business rule violations
- **GoalNotFoundException**: Goal not found errors
- **DurationCalculationException**: Activity duration calculation errors
- **ErrorWhileProcessing**: Generic processing errors

#### Error Response Enhancement
- **Field-Level Errors**: Specific field validation messages
- **Error Codes**: Standardized error identifiers
- **Error Details**: Additional context for debugging

### Validation Examples
```java
// Goal weight validation
if (consistencyWeight + momentumWeight + progressWeight != 100) {
    throw new ValidationException("Health weights must sum to 100");
}

// Date validation
if (targetDate.isBefore(startDate)) {
    throw new ValidationException("Target date cannot be before start date");
}
```

### Key Decisions
- Comprehensive field-level validation
- Standardized error response format
- Custom exceptions for different error types

---

## Phase 7: Performance Optimization and Indexing

**Objective**: Improve database performance through strategic indexing

### Features Added

#### Database Indexes
- **GoalWeeklySnapshot Indexes**:
  - `idx_snapshot_goal_week` on (goal_id, week_start)
  - `idx_snapshot_user_week` on (user_id, week_start)
  - `idx_snapshot_goal_id` on (goal_id)
- **Activity Indexes**:
  - `idx_activity_goal_id` on (goal_id)
- **Goal Indexes**:
  - Optimized queries for user-specific data

#### Query Optimization
- **Efficient Hierarchy Queries**: Optimized parent-child lookups
- **Bulk Operations**: Reduced database round trips
- **Connection Pooling**: Improved database connection management

### Index Strategy
```sql
-- Weekly snapshot performance indexes
CREATE INDEX idx_snapshot_goal_week ON goal_weekly_snapshots(goal_id, week_start);
CREATE INDEX idx_snapshot_user_week ON goal_weekly_snapshots(user_id, week_start);
CREATE INDEX idx_snapshot_goal_id ON goal_weekly_snapshots(goal_id);

-- Activity performance index
CREATE INDEX idx_activity_goal_id ON activities(goal_id);
```

### Key Decisions
- Strategic indexing based on query patterns
- Performance monitoring and optimization
- Minimal overhead for maximum benefit

---

## Phase 8: Health Summary and Statistics Enhancement

**Objective**: Add comprehensive health analytics and dashboard endpoints

### Features Added

#### Health Summary Endpoint
- **New Endpoint**: `GET /api/v1/goals/health-summary`
- **Health Distribution**: Count of goals by health status
- **Average Health Score**: Overall system health
- **Quick Overview**: Dashboard summary data

#### Enhanced Statistics
- **Health Metrics**: Added to existing statistics endpoint
- **Goal Classification**: Leaf vs parent goal statistics
- **Health Status Breakdown**: Detailed health distribution

#### Manual Health Recalculation
- **New Endpoint**: `PATCH /api/v1/goals/{id}/health/recalculate`
- **Debugging Support**: Manual health fixes
- **Admin Operations**: Health system maintenance

### Health Summary Response
```json
{
  "totalGoals": 10,
  "thrivingGoals": 3,
  "onTrackGoals": 4,
  "atRiskGoals": 2,
  "criticalGoals": 1,
  "untrackedGoals": 0,
  "averageHealthScore": 72.5,
  "healthDistribution": {
    "THRIVING": 3, "ON_TRACK": 4, "AT_RISK": 2, "CRITICAL": 1, "UNTRACKED": 0
  }
}
```

### Enhanced Statistics
```json
{
  // Existing fields...
  "thrivingGoals": 3,
  "onTrackGoals": 4, 
  "atRiskGoals": 2,
  "criticalGoals": 1,
  "untrackedGoals": 0,
  "averageHealthScore": 72.5,
  "trackedGoals": 8,
  "leafGoals": 7
}
```

### Key Decisions
- Separate health summary endpoint for quick dashboard loading
- Enhanced statistics with health metrics
- Manual health recalculation for debugging

---

## Phase 9: Evaluation Period System

**Objective**: Add flexible evaluation periods alongside existing weekly system

### Features Added

#### EvaluationPeriod Enum
- **Period Types**: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
- **Flexible Targeting**: Different time windows for consistency measurement
- **Backward Compatibility**: WEEKLY falls back to existing system

#### Goal Period Tracking
- **New Goal Fields**:
  - `evaluationPeriod` - Period type selection
  - `targetPerPeriod` - Target count per period
  - `customPeriodDays` - Days for CUSTOM periods
  - `currentPeriodStart` - Current period start date
  - `currentPeriodCount` - Activities in current period
  - `periodConsistencyScore` - Period-based consistency

#### Period Snapshot System
- **GoalPeriodSnapshot Entity**: Parallel to weekly snapshots
- **Period-Based Consistency**: Alternative to weekly consistency
- **Period Calculations**: Flexible period start/end logic

#### PeriodUtils Utility
- **Period Calculations**: Start/end dates for any period type
- **Custom Period Support**: N-day cycles with anchor dates
- **Momentum Support**: Previous period calculations

### Evaluation Period Examples
```json
// Daily practice goal
{
  "title": "Quantitative Practice",
  "evaluationPeriod": "DAILY",
  "targetPerPeriod": 2
}

// Monthly reading goal  
{
  "title": "Reading Challenge",
  "evaluationPeriod": "MONTHLY", 
  "targetPerPeriod": 4
}

// Custom 10-day cycle
{
  "title": "Study Cycle",
  "evaluationPeriod": "CUSTOM",
  "targetPerPeriod": 5,
  "customPeriodDays": 10
}
```

### Period Snapshot Schema
```sql
CREATE TABLE goal_period_snapshots (
    id BIGINT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    period_type VARCHAR(50) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    activities_logged INTEGER NOT NULL DEFAULT 0,
    target_per_period INTEGER,
    consistency_score DOUBLE PRECISION,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (goal_id, period_type, period_start)
);
```

### Key Decisions
- Parallel system (weekly + period snapshots)
- WEEKLY evaluationPeriod uses existing system
- Comprehensive period calculation utilities
- Backward compatibility maintained

---

## Future Phases (Planned)

### Phase 10: Advanced Analytics and Reporting
- **Trend Analysis**: Long-term health trends
- **Predictive Analytics**: Goal completion predictions
- **Custom Reports**: User-defined report templates
- **Export Functionality**: Data export capabilities

### Phase 11: Mobile App Support
- **Mobile API**: Optimized endpoints for mobile
- **Push Notifications**: Goal reminders and achievements
- **Offline Support**: Sync capabilities
- **Mobile UI Guidelines**: Responsive design patterns

### Phase 12: Multi-Target Evaluation Periods (Option B)
- **Multiple Targets**: Multiple targets per evaluation period
- **Complex Configurations**: Advanced goal setups
- **Target Types**: Different target types per period
- **Advanced Analytics**: Multi-target health calculations

### Phase 13: Social Features
- **Goal Sharing**: Share goals with others
- **Team Goals**: Collaborative goal tracking
- **Leaderboards**: Competitive tracking
- **Achievement System**: Badges and milestones

### Phase 14: Integration Ecosystem
- **Third-party Integrations**: Calendar, fitness apps
- **Webhooks**: Real-time notifications
- **API Extensions**: Public API for developers
- **Data Import**: Migration tools from other platforms

---

## Migration Guide

### Database Migration Path
1. **Phase 1**: Core goals and health system
2. **Phase 2**: Add parent_goal_id column
3. **Phase 9**: Add evaluation period columns
4. **Future Phases**: Additional columns as needed

### API Evolution
- **Backward Compatibility**: Maintained across phases
- **Version Strategy**: /api/v1/ prefix for future versions
- **Deprecation Policy**: 6-month deprecation notice
- **Breaking Changes**: Major version increments only

### Data Migration Strategies
- **Gradual Rollout**: Feature flags for new functionality
- **Data Validation**: Consistency checks during migration
- **Rollback Plans**: Ability to revert changes
- **Performance Monitoring**: Track migration impact

---

## Technical Debt and Improvements

### Current Technical Debt
- **Lombok Usage**: Mixed usage (some entities use Lombok, others don't)
- **Exception Handling**: Could be more standardized
- **Testing Coverage**: Needs improvement across phases
- **Documentation**: API documentation could be more comprehensive

### Planned Improvements
- **Standardize Entity Patterns**: Consistent Lombok usage
- **Enhanced Error Handling**: Global exception handler
- **Comprehensive Testing**: Unit, integration, and E2E tests
- **API Documentation**: OpenAPI/Swagger integration
- **Performance Monitoring**: APM integration
- **Security Enhancements**: Rate limiting, input validation

### Architecture Evolution
- **Microservices**: Potential service splitting
- **Event-Driven Architecture**: Kafka-based event system
- **CQRS**: Read/write model separation
- **Caching Strategy**: Redis integration for performance

---

## Conclusion

The Northstar Activity Tracker has evolved through 9 phases, each building upon the previous while maintaining backward compatibility. The system now provides:

- **Comprehensive Goal Tracking**: Hierarchical goals with health scoring
- **Flexible Evaluation Periods**: Multiple time windows for consistency measurement
- **Robust Health Engine**: Three-component health scoring with rollup
- **Rich API Surface**: Complete CRUD operations with analytics
- **Performance Optimization**: Strategic indexing and query optimization

The phased approach has allowed for iterative development, testing, and refinement while maintaining system stability and user experience quality.
