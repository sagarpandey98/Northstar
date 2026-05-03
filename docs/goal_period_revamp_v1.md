# Goal Period Revamp V1

## What Changed

Goal Period is now treated as a lightweight period snapshot, not a second copy of Goal.

Fields removed from the entity because they should come from the parent Goal:

- `completedDate`
- `targetOperator`
- `metric`
- `consistencyWeight`
- `momentumWeight`
- `progressWeight`
- `minimumSessionPeriod`
- `maximumSessionPeriod`
- `missesAllowedPerPeriod`
- `allowDoubleLogging`

Fields retained in Goal Period:

- `id`
- `uuid`
- `goalId` (`goal_id` / parent goal UUID)
- `periodStart`
- `periodEnd`
- `currentValue`
- `progressPercentage`
- `healthStatus`
- `healthScore`
- `consistencyScore`
- `momentumScore`
- `progressScore`
- `currentStreak`
- `longestStreak`
- `minimumSessionDaily`
- `scheduleSpec`
- `createdAt`
- `lastUpdatedAt`

## Parent Relationship

`GoalPeriod` now has a real `ManyToOne` relationship to `Goal`, while still keeping `goal_id` as the stored UUID link.

This gives us:

- scalable DB navigation
- cleaner access to parent-owned configuration
- less duplicate data inside the period snapshot

## Lifecycle Improvements

The new lifecycle is self-healing and proactive:

1. the first period is created automatically when a trackable goal is created
2. reading the active period can auto-create missing periods up to the requested date
3. bulk creation can fill historical gaps or create forward periods
4. next-period creation is supported explicitly
5. overlapping periods for the same goal are blocked

Current implementation note:

- health calculations use a placeholder function inside Goal Period service for now
- later this can be delegated fully to the health service once that revamp is done

## API Surface

All APIs are nested under a goal:

### List periods

`GET /api/v1/goals/{goalUuid}/periods`

### Get active period

`GET /api/v1/goals/{goalUuid}/periods/active?date=YYYY-MM-DD`

This endpoint is self-healing and will create the missing active period if needed.

### Get one period

`GET /api/v1/goals/{goalUuid}/periods/{periodUuid}`

### Create one period

`POST /api/v1/goals/{goalUuid}/periods`

Behavior:

- if `periodStart` and `periodEnd` are provided, create that custom period
- otherwise create the next period in sequence

Request body:

```json
{
  "periodStart": "2026-05-01",
  "periodEnd": "2026-05-31",
  "currentValue": 0,
  "scheduleSpec": {}
}
```

### Bulk create periods

`POST /api/v1/goals/{goalUuid}/periods/bulk`

Request body:

```json
{
  "startDate": "2026-01-01",
  "throughDate": "2026-12-31",
  "maxPeriods": 12,
  "fillGaps": true
}
```

### Reconcile / self-heal periods

`POST /api/v1/goals/{goalUuid}/periods/reconcile?throughDate=YYYY-MM-DD`

### Update period

`PUT /api/v1/goals/{goalUuid}/periods/{periodUuid}`

Request body:

```json
{
  "periodStart": "2026-05-01",
  "periodEnd": "2026-05-31",
  "currentValue": 12,
  "scheduleSpec": {}
}
```

### Delete period

`DELETE /api/v1/goals/{goalUuid}/periods/{periodUuid}`

## Frontend Notes

Frontend should treat Goal Period as:

- a date window snapshot
- with progress and health values
- but not the source of goal rules like `metric`, `targetOperator`, `allowDoubleLogging`, or period min/max bounds

Those should still be read from the parent Goal.

## Known Follow-up

The next good step is to move placeholder period health calculation into the dedicated health engine once that service is redesigned.
