# Goal Health Frontend Notes

## Payload Shape

There is no new health API payload shape in this revamp.

Existing fields remain the same:

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

## No Required Frontend Code Change

No required request or response contract change was introduced in this revamp.

Frontend only needs to account for the fact that the same fields now have better semantics and may move more realistically for schedule-specific goals.
