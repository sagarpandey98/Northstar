# Goal Health Frontend Notes

## Existing Health Fields

Existing goal and period health fields remain the same:

- `goal.consistencyScore`
- `goal.momentumScore`
- `goal.progressScore`
- `goal.healthScore`
- `goal.healthStatus`
- `goalPeriod.consistencyScore`
- `goalPeriod.momentumScore`
- `goalPeriod.progressScore`
- `goalPeriod.healthScore`
- `goalPeriod.healthStatus`

## Semantic Change

The meaning of the health fields is now stronger and more schedule-aware.

Before:

- health was mostly based on coarse period totals

Now:

- health is derived from day-level expectation vs day-level actual activity
- schedule-specific goals are scored honestly
- flexible schedules are distributed evenly across their valid scope
- parent goal health is a weighted rollup from child goals

## Frontend Recommendations

1. trust `healthStatus` more directly for chips, badges, and list emphasis
2. use `goalPeriod` views when showing historical health trends
3. avoid explaining health as simple percent-of-target; it is now a composite of consistency, momentum, and progress
4. if you show tooltip/help text, describe the metrics as:
   - consistency: how much of the expected work pattern was met
   - progress: how much of the target work pattern was met
   - momentum: how current pace compares to recent periods

## Period Health Breakdown Endpoint

Frontend can now fetch an explainable period-level health payload:

```text
GET /api/v1/goals/{goalIdOrUuid}/periods/{periodUuid}/health-breakdown
```

Optional:

```text
?evaluationDate=YYYY-MM-DD
```

Use this for period drill-down screens, health tooltips, and "why is this period AT_RISK?" views.

Important fields:

- `unitLabel`: `activities` for `COUNT`, `minutes` for `DURATION`
- `expectedMinimumUnitsToDate`: consistency expectation so far
- `expectedTargetUnitsToDate`: progress expectation so far
- `actualUnitsToDate`: actual completed units so far
- `dailyDetails`: expected-vs-actual rows for every date in the period
- `momentumBreakdown`: trend metadata comparing this period to recent periods

Daily detail fields:

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

## Momentum Display

Use `momentumBreakdown.trend` for user-facing trend states:

- `FIRST_PERIOD`: no past comparison yet
- `IMPROVING`: current period is at or above recent baseline
- `DECLINING`: current period is below recent baseline
- `RECOVERING`: current period is above a zero baseline
- `FLAT_ZERO`: no movement yet
- `NO_BASELINE`: insufficient comparable periods
- `UNTRACKED`: no score yet

`momentumBreakdown.deltaFromBaseline` can power small "up/down from recent average" UI labels.

## Import/Recalculate Flow

After CSV imports or admin data repairs, frontend/admin tooling should trigger:

```text
POST /api/v1/goals/{goalIdOrUuid}/periods/health/recalculate
```

To fill missing periods before recalculation:

```text
POST /api/v1/goals/{goalIdOrUuid}/periods/health/recalculate?reconcile=true&throughDate=YYYY-MM-DD
```

For one period:

```text
POST /api/v1/goals/{goalIdOrUuid}/periods/{periodUuid}/health/recalculate
```

## No Required Frontend Code Change

No existing request or response contract was broken.

Frontend only needs to account for the fact that the same fields now have better semantics and may move more realistically for schedule-specific goals.
