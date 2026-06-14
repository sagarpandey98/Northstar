# Goal Health Engine Revamp

## Purpose

This revamp replaces the old period-total health calculation with an expectation-driven model based on `schedule_spec`.

The new engine answers:

- what should the user ideally have done on each day of a goal period
- how much did the user actually do on each day
- how consistent and progressive the user is relative to the configured schedule
- whether the current period is stronger or weaker than recent periods

## Core Design

Health is calculated in a strict hierarchy:

1. `GoalPeriod` is the source of truth for health math
2. leaf `Goal` health is rolled up from all of its periods
3. parent `Goal` health is rolled up from its direct children

This keeps schedule logic local to the period while preserving historical snapshots.

## New Expectation Service

Implementation:

- `GoalPeriodExpectationService`
- `GoalPeriodExpectationServiceV1`
- `GoalPeriodExpectation`
- `GoalDayExpectation`

Input:

- parent `Goal`
- target `GoalPeriod`
- optional evaluation date

Output:

- one expectation row per day in the period
- period totals
- to-date totals

Each day contains:

- `date`
- `actionable`
- `expectedMinimumUnits`
- `expectedTargetUnits`

## How Daily Expectation Is Derived

The engine distributes the parent goal's:

- `minimumSessionPeriod` for consistency
- `maximumSessionPeriod` for progress

across the dates in the period according to `schedule_spec`.

### Distribution Rules

1. if no rules exist, the period is flexible and units are spread evenly across actionable days
2. sibling rules split the parent allocation equally
3. a rule with multiple `values` behaves like multiple equal sibling branches
4. a `FLEXIBLE` branch spreads its share evenly across all matched dates in its scope
5. a `STRICT` branch pushes its share down into child rules
6. time-of-day and time-window rules create multiple branches on the same date, which raises expectation for that day
7. exclusions remove dates before allocation

### Examples

`WEEKLY` flexible with `minimumSessionPeriod = 7`

- expected minimum per day = `1`

`MONTHLY` with:

- `W1 -> MONDAY, FRIDAY`
- `W3 -> FLEXIBLE`
- `minimumSessionPeriod = 6`

Distribution:

- root has 2 branches, so each gets `3`
- `W1` has 2 selected days, so each gets `1.5`
- `W3` has 7 bucket days, so each gets `3/7`

## Actual Activity Aggregation

Actual activity is aggregated per local day in the schedule timezone.

### COUNT and CUSTOM

- each activity contributes `1` unit
- units are assigned to the activity's local start date

### DURATION

- activity duration is split across local date boundaries
- only the overlapping portion inside the period is counted

This makes duration goals much more accurate for sessions crossing midnight.

## Period Consistency

Consistency compares what the user should have done by now to what they actually did by now.

Formula:

`sum(min(actualDayUnits, expectedMinimumDayUnits)) / sum(expectedMinimumDayUnits) * 100`

Important:

- fulfillment is capped per day
- over-performing on Monday does not erase missing Wednesday for a specific schedule

If there is no elapsed minimum expectation yet, consistency is `null`.

## Period Progress

Progress is calculated the same way, but against target units.

Formula:

`sum(min(actualDayUnits, expectedTargetDayUnits)) / sum(expectedTargetDayUnits) * 100`

If there is no elapsed target expectation yet, progress is `null`.

## Period Momentum

Momentum compares the current period's performance against recent periods.

Current period base score:

`average(consistency, progress)` using whichever of those two are available

Rules:

- first scored period -> `100`
- one historical period -> compare against that single period
- two or more historical periods -> compare against the average of the last two

Formula:

- if current base >= historical base, momentum = `100`
- else momentum = `current / historical * 100`

If the current period has no scored consistency/progress yet, momentum is `null`.

## Period Health Score

Period health is the weighted average of:

- consistency
- momentum
- progress

Weights come from the parent goal:

- `consistencyWeight`
- `momentumWeight`
- `progressWeight`

If one component is unavailable, the remaining weights are normalized over the available components only.

## Period Health Status

Derived from period `healthScore`:

- `THRIVING` >= 80
- `ON_TRACK` >= 60
- `AT_RISK` >= 40
- `CRITICAL` < 40
- `UNTRACKED` when no elapsed expectation and no activity exist yet

## Leaf Goal Rollup

Leaf goal fields are rolled up as simple averages across scored periods:

- `consistencyScore`
- `momentumScore`
- `progressScore`
- `healthScore`

`healthStatus` is derived from the rolled-up `healthScore`.

The health engine no longer overwrites `goal.progressPercentage`. That field remains separate from health math.

## Parent Goal Rollup

Parent goals do not derive health from their own periods.

They roll up from direct child goals using priority weights:

- `CRITICAL = 4`
- `HIGH = 3`
- `MEDIUM = 2`
- `LOW = 1`

The same weighted rollup is used for:

- consistency
- momentum
- progress
- health

## Streak Logic

Streak is period-based.

A period counts as a hit when:

- `consistencyScore >= 100`

Misses are tolerated according to:

- `missesAllowedPerPeriod`

Future or untracked periods do not consume misses.

## Important Behavioral Changes

1. health is now schedule-aware
2. specific schedules cannot be gamed by bunching all work into one day
3. period activity uses schedule timezone instead of flat UTC day grouping
4. parent health is a weighted child rollup
5. period and goal `healthStatus` are updated from the real health engine instead of placeholder math

## Period Health Breakdown API

The health engine now exposes the same internals it uses for calculation through a period-level breakdown endpoint:

```text
GET /api/v1/goals/{goalIdOrUuid}/periods/{periodUuid}/health-breakdown
GET /api/v1/goals/{goalIdOrUuid}/periods/{periodUuid}/health-breakdown?evaluationDate=YYYY-MM-DD
```

The response includes:

- goal and period identifiers
- period start/end and evaluation date
- metric and `unitLabel`
- consistency, progress, momentum, and health scores
- health status
- score weights
- expected minimum/target units for the period
- expected minimum/target units to date
- actual units for the period and to date
- daily expected-vs-actual details
- momentum trend metadata

### Unit Semantics

Health math uses generic `units`.

For `COUNT` goals:

- `1 unit = 1 activity/check-in`

For `DURATION` goals:

- `1 unit = 1 minute`

This keeps the formulas identical across goal types while allowing the frontend to display the correct label.

### Daily Detail Semantics

Each daily detail row contains:

- `date`
- `actionable`
- `countedInScore`
- `expectedMinimumUnits`
- `expectedTargetUnits`
- `actualUnits`
- `consistencyFulfilledUnits`
- `progressFulfilledUnits`
- `consistencyScore`
- `progressScore`

Daily fulfillment is capped:

```text
consistencyFulfilledUnits = min(actualUnits, expectedMinimumUnits)
progressFulfilledUnits = min(actualUnits, expectedTargetUnits)
```

This is what prevents a user from bunching all work into one strict schedule day and receiving full consistency.

### Momentum Trend Semantics

Momentum still compares the current period composite against the previous one or two periods, but the API now exposes the comparison:

- `currentCompositeScore`
- `baselineCompositeScore`
- `deltaFromBaseline`
- `periodsCompared`
- `trend`
- `explanation`

Valid trends:

- `FIRST_PERIOD`
- `NO_BASELINE`
- `IMPROVING`
- `DECLINING`
- `RECOVERING`
- `FLAT_ZERO`
- `UNTRACKED`

## Import And Recalculation Workflow

CSV imports and direct database writes do not automatically run Java health calculation. After importing activities or periods, call:

```text
POST /api/v1/goals/{goalIdOrUuid}/periods/health/recalculate
```

Optional reconciliation before recalculation:

```text
POST /api/v1/goals/{goalIdOrUuid}/periods/health/recalculate?reconcile=true&throughDate=YYYY-MM-DD
```

For one specific period:

```text
POST /api/v1/goals/{goalIdOrUuid}/periods/{periodUuid}/health/recalculate
```

The goal-level recalculation path still rolls health up to parent goals through the health service.

## Files Changed

- `src/main/java/com/sagarpandey/activity_tracker/Service/Interface/GoalPeriodExpectationService.java`
- `src/main/java/com/sagarpandey/activity_tracker/Service/V1/GoalPeriodExpectationServiceV1.java`
- `src/main/java/com/sagarpandey/activity_tracker/dtos/health/GoalDayExpectation.java`
- `src/main/java/com/sagarpandey/activity_tracker/dtos/health/GoalPeriodExpectation.java`
- `src/main/java/com/sagarpandey/activity_tracker/dtos/health/GoalDayHealthDetail.java`
- `src/main/java/com/sagarpandey/activity_tracker/dtos/health/GoalPeriodHealthBreakdown.java`
- `src/main/java/com/sagarpandey/activity_tracker/dtos/health/MomentumBreakdown.java`
- `src/main/java/com/sagarpandey/activity_tracker/Service/V1/GoalHealthServiceV2.java`
- `src/main/java/com/sagarpandey/activity_tracker/Repository/ActivityRepository.java`
- `src/main/java/com/sagarpandey/activity_tracker/Service/V1/GoalPeriodServiceV1.java`
- `src/main/java/com/sagarpandey/activity_tracker/controllers/GoalPeriodController.java`

## Tests Added

- `GoalPeriodExpectationServiceV1Test`
- `GoalHealthServiceV2Test`

These cover:

- flexible weekly distribution
- mixed strict/flexible monthly distribution
- deep yearly nesting
- anti-bunching consistency behavior
- momentum vs last two periods
- parent weighted rollup
