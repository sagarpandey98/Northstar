# Data Model

## Activity

**Table Name**: `activities`

**Columns**:
- `id` (BIGINT, NOT NULL, Primary Key) - Auto-increment identifier
- `uuid` (UUID, NOT NULL) - Internal UUID for references
- `user_id` (VARCHAR(255), NOT NULL) - User who owns this activity
- `name` (VARCHAR) - Activity name/title
- `start_time` (TIMESTAMP WITH TIME ZONE) - When activity started
- `duration` (VARCHAR) - Duration as string
- `end_time` (TIMESTAMP WITH TIME ZONE) - When activity ended
- `description` (VARCHAR) - Activity description
- `domain_name` (VARCHAR) - Domain name (stored as value)
- `subdomain_name` (VARCHAR) - Subdomain name (stored as value)
- `specific_name` (VARCHAR) - Specific name (stored as value)
- `domain_id` (VARCHAR) - Domain UUID reference
- `subdomain_id` (VARCHAR) - Subdomain UUID reference
- `specific_id` (VARCHAR) - Specific UUID reference
- `mood` (INTEGER) - Subjective mood rating (1-5)
- `rating` (INTEGER) - Subjective activity rating (1-5)
- `source` (VARCHAR) - Source of activity creation
- `goal_id` (BIGINT, NULLABLE) - Optional reference to Goal.id
- `created_at` (TIMESTAMP) - Record creation time
- `updated_at` (TIMESTAMP) - Record update time
- `is_deleted` (BOOLEAN) - Soft delete flag

**What it represents**: Individual activity records logged by users, representing completed tasks, exercises, or any tracked action. Activities can be optionally linked to goals for health score calculation.

**Relationships**: 
- Optional many-to-one relationship with Goal (via goal_id)
- References Domain/Subdomain/Specific by name and UUID (stored as values)

**Indexes**:
- `idx_activity_goal_id` on goal_id column

**Special Behavior**: No @PrePersist/@PreUpdate methods

---

## Domain

**Table Name**: `domains` (extends BaseModel)

**Columns**:
- `id` (BIGINT, NOT NULL, Primary Key) - Auto-increment identifier
- `uuid` (UUID, NOT NULL) - Internal UUID for references
- `user_id` (VARCHAR(255), NOT NULL) - User who owns this domain
- `name` (VARCHAR) - Domain name
- `description` (VARCHAR) - Domain description
- `created_at` (TIMESTAMP) - Record creation time
- `updated_at` (TIMESTAMP) - Record update time
- `is_deleted` (BOOLEAN) - Soft delete flag

**What it represents**: Top-level categories for organizing goals and activities (e.g., "Health", "Career", "Education").

**Relationships**: 
- One-to-many with Subdomain (parent-child relationship)

**Indexes**: None specified beyond primary key

**Special Behavior**: Inherits from BaseModel with standard audit fields

---

## Subdomain

**Table Name**: `subdomains` (extends BaseModel)

**Columns**:
- `id` (BIGINT, NOT NULL, Primary Key) - Auto-increment identifier
- `uuid` (UUID, NOT NULL) - Internal UUID for references
- `user_id` (VARCHAR(255), NOT NULL) - User who owns this subdomain
- `name` (VARCHAR) - Subdomain name
- `description` (VARCHAR) - Subdomain description
- `domain_id` (BIGINT, Foreign Key) - Reference to Domain.id
- `created_at` (TIMESTAMP) - Record creation time
- `updated_at` (TIMESTAMP) - Record update time
- `is_deleted` (BOOLEAN) - Soft delete flag

**What it represents**: Mid-level categories that belong to Domains (e.g., "Fitness" under "Health", "CAT Prep" under "Education").

**Relationships**: 
- Many-to-one with Domain (via domain_id)
- One-to-many with Specific (parent-child relationship)

**Indexes**: 
- Foreign key constraint on domain_id

**Special Behavior**: Inherits from BaseModel with standard audit fields

---

## Specifics

**Table Name**: `specifics` (extends BaseModel)

**Columns**:
- `id` (BIGINT, NOT NULL, Primary Key) - Auto-increment identifier
- `uuid` (UUID, NOT NULL) - Internal UUID for references
- `user_id` (VARCHAR(255), NOT NULL) - User who owns this specific
- `name` (VARCHAR) - Specific name
- `description` (VARCHAR) - Specific description
- `subdomain_id` (BIGINT, Foreign Key) - Reference to Subdomain.id
- `created_at` (TIMESTAMP) - Record creation time
- `updated_at` (TIMESTAMP) - Record update time
- `is_deleted` (BOOLEAN) - Soft delete flag

**What it represents**: Bottom-level categories for fine-grained organization (e.g., "Running" under "Fitness", "Quantitative" under "CAT Prep").

**Relationships**: 
- Many-to-one with Subdomain (via subdomain_id)

**Indexes**: 
- Foreign key constraint on subdomain_id

**Special Behavior**: Inherits from BaseModel with standard audit fields

---

## Goal

**Table Name**: `goals`

**Columns**:
- `id` (BIGINT, NOT NULL, Primary Key) - Auto-increment identifier
- `uuid` (VARCHAR, NOT NULL, Unique) - Internal UUID for references
- `user_id` (VARCHAR(255), NOT NULL) - User who owns this goal
- `title` (VARCHAR, NOT NULL) - Goal title
- `description` (VARCHAR(1000)) - Goal description
- `notes` (VARCHAR(1000)) - Goal notes
- `priority` (VARCHAR, NOT NULL) - Priority level (LOW, MEDIUM, HIGH, CRITICAL)
- `status` (VARCHAR, NOT NULL) - Goal status (NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE)
- `metric` (VARCHAR, NOT NULL) - Measurement type (COUNT, DURATION, CUSTOM)
- `target_operator` (VARCHAR, NOT NULL) - Target comparison (GREATER_THAN, EQUAL, LESS_THAN)
- `target_value` (DOUBLE, NOT NULL) - Target value to achieve
- `current_value` (DOUBLE, NOT NULL, Default 0.0) - Current progress value
- `progress_percentage` (DOUBLE, NOT NULL, Default 0.0) - Calculated progress percentage
- `start_date` (TIMESTAMP) - Goal start date
- `target_date` (TIMESTAMP) - Goal target date
- `completed_date` (TIMESTAMP) - Goal completion date
- `parent_goal_id` (VARCHAR) - UUID reference to parent goal
- `is_milestone` (BOOLEAN, Default false) - Milestone flag
- `created_at` (TIMESTAMP) - Record creation time
- `updated_at` (TIMESTAMP) - Record update time
- `is_deleted` (BOOLEAN, Default false) - Soft delete flag

**Phase 1 Fields**:
- `goal_type` (VARCHAR(50)) - Goal type (HABIT, PROJECT, SKILL, FITNESS, GENERAL)
- `target_frequency_weekly` (INTEGER) - Target activities per week
- `target_volume_daily` (INTEGER) - Target volume per day
- `schedule_type` (VARCHAR(50)) - Schedule type (FLEXIBLE, SPECIFIC_DAYS)
- `schedule_days` (VARCHAR(100)) - Comma-separated schedule days (e.g., "MON,WED,FRI")
- `minimum_session_minutes` (INTEGER) - Minimum session duration
- `allow_double_logging` (BOOLEAN) - Allow multiple activities per day
- `misses_allowed_per_week` (INTEGER) - Allowed misses per week
- `misses_allowed_per_month` (INTEGER) - Allowed misses per month
- `consistency_weight` (INTEGER) - Weight for consistency score (0-100)
- `momentum_weight` (INTEGER) - Weight for momentum score (0-100)
- `progress_weight` (INTEGER) - Weight for progress score (0-100)
- `consistency_score` (DOUBLE PRECISION) - Calculated consistency score
- `momentum_score` (DOUBLE PRECISION) - Calculated momentum score
- `health_score` (DOUBLE PRECISION) - Overall health score (0-100)
- `health_status` (VARCHAR(50)) - Health status (THRIVING, ON_TRACK, AT_RISK, CRITICAL, UNTRACKED)
- `current_streak` (INTEGER) - Current streak count
- `longest_streak` (INTEGER) - Historical best streak

**Phase 9 Fields**:
- `evaluation_period` (VARCHAR(50)) - Period type (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM)
- `target_per_period` (INTEGER) - Target count per evaluation period
- `custom_period_days` (INTEGER) - Days in custom period
- `current_period_start` (DATE) - Start of current evaluation period
- `current_period_count` (INTEGER) - Activities in current period
- `period_consistency_score` (DOUBLE PRECISION) - Period-based consistency score

**What it represents**: Core goal entities with hierarchical structure, health tracking, and flexible evaluation periods. Goals can have parent-child relationships and support multiple tracking strategies.

**Relationships**: 
- Self-referential hierarchy via parent_goal_id (UUID references)
- One-to-many with Activity (via goal_id, optional)
- One-to-many with GoalWeeklySnapshot
- One-to-many with GoalPeriodSnapshot

**Constraints**:
- Unique constraint on uuid
- Foreign key relationships through application logic (not DB constraints)

**Indexes**: None specified beyond primary key and unique uuid

**Special Behavior**: 
- @PreUpdate method updates lastUpdatedAt
- Helper methods for period calculations and weight resolution

---

## GoalWeeklySnapshot

**Table Name**: `goal_weekly_snapshots`

**Columns**:
- `id` (BIGINT, NOT NULL, Primary Key) - Auto-increment identifier
- `goal_id` (BIGINT, NOT NULL) - Reference to Goal.id
- `user_id` (VARCHAR(255), NOT NULL) - User who owns this snapshot
- `week_start` (DATE, NOT NULL) - Monday of the week
- `activities_logged` (INTEGER, NOT NULL, Default 0) - Activities logged this week
- `target_frequency_weekly` (INTEGER) - Target activities per week (snapshot)
- `consistency_score_for_week` (DOUBLE PRECISION) - Consistency score for this week
- `created_at` (TIMESTAMP) - Record creation time
- `updated_at` (TIMESTAMP) - Record update time

**What it represents**: Weekly activity snapshots for momentum calculation and health scoring. One record per goal per week.

**Relationships**: 
- Many-to-one with Goal (via goal_id)

**Constraints**:
- Unique constraint on (goal_id, week_start)

**Indexes**:
- `idx_snapshot_goal_week` on (goal_id, week_start)
- `idx_snapshot_user_week` on (user_id, week_start)
- `idx_snapshot_goal_id` on (goal_id)

**Special Behavior**: 
- @PrePersist initializes activities_logged to 0
- @PreUpdate updates updated_at timestamp

---

## GoalPeriodSnapshot

**Table Name**: `goal_period_snapshots`

**Columns**:
- `id` (BIGINT, NOT NULL, Primary Key) - Auto-increment identifier
- `goal_id` (BIGINT, NOT NULL) - Reference to Goal.id
- `user_id` (VARCHAR(255), NOT NULL) - User who owns this snapshot
- `period_type` (VARCHAR(50), NOT NULL) - Period type (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM)
- `period_start` (DATE, NOT NULL) - Start date of the period
- `period_end` (DATE, NOT NULL) - End date of the period (inclusive)
- `activities_logged` (INTEGER, NOT NULL, Default 0) - Activities logged in this period
- `target_per_period` (INTEGER) - Target count per period (snapshot)
- `consistency_score` (DOUBLE PRECISION) - Consistency score for this period
- `created_at` (TIMESTAMP) - Record creation time
- `updated_at` (TIMESTAMP) - Record update time

**What it represents**: Period-based activity snapshots for alternative evaluation periods alongside weekly tracking. Supports daily, monthly, quarterly, yearly, and custom periods.

**Relationships**: 
- Many-to-one with Goal (via goal_id)

**Constraints**:
- Unique constraint on (goal_id, period_type, period_start)

**Indexes**:
- `idx_period_snapshot_goal` on (goal_id, period_type, period_start)
- `idx_period_snapshot_user` on (user_id, period_type, period_start)

**Special Behavior**: 
- @PrePersist initializes activities_logged to 0
- @PreUpdate updates updated_at timestamp

---

## Entity Relationship Diagram

```
Domain ||--o{ Subdomain : "has many"
Subdomain ||--o{ Specifics : "has many"

Goal ||--o{ GoalWeeklySnapshot : "has weekly snapshots"
Goal ||--o{ GoalPeriodSnapshot : "has period snapshots"
Goal ||--o{ Goal : "parent-child hierarchy (parent_goal_id)"

Activity }o--|| Goal : "linked to (optional goal_id)"
Activity }o--|| Domain : "references (domain_name, domain_id)"
Activity }o--|| Subdomain : "references (subdomain_name, subdomain_id)"
Activity }o--|| Specifics : "references (specific_name, specific_id)"
```

**Key Relationships**:
- Goals form hierarchical trees via parent_goal_id (UUID references)
- Activities can optionally link to goals for health calculation
- Category hierarchy: Domain → Subdomain → Specific
- Snapshots provide historical data for health calculations
- Weekly and Period snapshots operate independently
