# Smart Todo V1.1 Implementation

## Goal

Revamp Smart Todo so it is:

- more accurate
- easier to explain
- aligned with `schedule_spec` V2
- directly useful to frontend without extra inference

## Core Design Choice

The revamp does **not** use ML or hidden heuristics. It uses a deterministic engine with explicit signals.

That decision came from two places:

1. the existing project already has enough structured data to do much better
2. smart task systems work best when users can understand why an item was surfaced

## Data Sources Used

Smart Todo V1.1 uses:

- `GoalResponse`
- `GoalPeriod`
- `ScheduleSpec`
- linked `ActivityResponse`

### Batch loading changes

The service now batch-loads periods through:

- `GoalPeriodRepository.findByParentGoalUuidIn(...)`

This removes repeated per-goal active-period lookups from the old flow.

## Evaluation Pipeline

For each trackable goal:

1. resolve user timezone from `scheduleSpec.timezone`, fallback to system timezone
2. find active goal period for the evaluated date
3. evaluate whether the date is actionable through `ScheduleSpecEvaluator`
4. measure daily progress from real activities
5. measure period progress from real activities
6. compute expected pace inside the active period
7. assign a bucket
8. build reason codes/messages
9. compute urgency score and score breakdown
10. sort all items and mark top incomplete items as recommended focus

## Bucket Rules

### `MUST_DO_TODAY`

Used when:

- the goal is explicitly scheduled for the evaluated date
- or the goal streak is at risk

### `CATCH_UP_TODAY`

Used when:

- the goal is behind expected pace for its active period
- but is not already in the strict must-do bucket

### `GOOD_TO_DO_TODAY`

Used when:

- the goal is flexible
- or on track
- and still worth surfacing

### `COMPLETED_TODAY`

Used when:

- daily target for the evaluated date has already been met

## Progress Model

### Daily progress

- `COUNT` -> number of linked activities on the date
- `DURATION` -> sum of duration minutes from linked activities
- `CUSTOM` -> currently approximated from activity count

### Period progress

Calculated from linked activities inside the active period window.

### Daily target

Derived from:

- explicit daily minimums when available
- or period target distributed across remaining actionable days
- then increased if catch-up is needed

## Schedule Explanation

The service builds human-readable schedule context from matching `schedule_spec` rules.

Examples:

- `WEEKLY • FRI • 3 check-ins / period`
- `YEARLY • Q1 > M1 > W3 > MON`

These are exposed through:

- `scheduleLabel`
- `scheduleDetails`

## Urgency Score

The score is still present, but it is no longer the primary user-facing model.

It is composed from:

- goal priority
- bucket
- streak risk
- behind pace
- deadline pressure
- low health score
- already-started bonus

Frontend should use:

1. bucket
2. display rank
3. recommended focus

before trying to interpret the raw score itself.

## New API Shape

The service now returns `SmartTodoListResponse` instead of a raw list.

That wrapper adds:

- evaluated date
- timezone
- list type
- summary counts
- recommended focus ids/titles
- item list

## Files Updated

Primary code changes:

- `src/main/java/com/sagarpandey/activity_tracker/Service/V1/SmartTodoServiceV1.java`
- `src/main/java/com/sagarpandey/activity_tracker/dtos/SmartTodoResponse.java`
- `src/main/java/com/sagarpandey/activity_tracker/dtos/SmartTodoListResponse.java`
- `src/main/java/com/sagarpandey/activity_tracker/Service/Interface/SmartTodoService.java`
- `src/main/java/com/sagarpandey/activity_tracker/controllers/TodoController.java`
- `src/main/java/com/sagarpandey/activity_tracker/Repository/GoalPeriodRepository.java`

## Known Limitations

1. `CUSTOM` metric accuracy is still limited by the current activity payload.
2. Future `/date` responses are planning-oriented, not predictive simulations.
3. For flexible goals without explicit schedule rules, Smart Todo still surfaces them as candidates for the day rather than hiding them entirely.

## Why This Is Better Than Old V1

Old V1 problems:

- lower-signal ranking
- weak completion logic
- date matching hack for recent activities
- poor frontend explainability
- too much hidden decision-making in one score

New V1.1 improvements:

- real bucket model
- pace-aware prioritization
- timezone-correct activity date matching
- richer response contract
- better docs for frontend
