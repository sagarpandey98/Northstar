# Smart Todo V1.1

## Overview

Smart Todo V1.1 answers a tighter question than the old version:

`What should I do for this date, and why?`

The service now uses:

- `schedule_spec` V2 as the source of scheduling truth
- active `GoalPeriod` windows for pace calculations
- real linked activities for progress
- explicit buckets instead of one opaque ranking

It still uses the same endpoints:

```text
GET  /api/v1/todos/today
GET  /api/v1/todos/date?date=YYYY-MM-DD
POST /api/v1/todos/refresh
```

But the response is now a wrapper object:

```json
{
  "date": "2026-04-25",
  "timezone": "Asia/Kolkata",
  "listType": "TODAY",
  "summary": {
    "totalItems": 4,
    "mustDoTodayCount": 1,
    "catchUpTodayCount": 1,
    "goodToDoTodayCount": 1,
    "completedTodayCount": 1,
    "recommendedFocusGoalIds": [12, 19, 31],
    "recommendedFocusTitles": ["Run", "Read", "Spanish"]
  },
  "items": []
}
```

## Bucket Model

Each todo item is placed into one of four buckets:

- `MUST_DO_TODAY`
  - hard-scheduled today
  - daily goals count here every day
  - rule-driven schedules count here only when the selected date matches `schedule_spec`
  - or streak is at risk
- `CATCH_UP_TODAY`
  - not strictly due today, but behind pace
- `GOOD_TO_DO_TODAY`
  - flexible or on-track work worth doing
- `COMPLETED_TODAY`
  - recommended target for the date is already met

Frontend should treat these buckets as the primary grouping model.

## Item Highlights

Each `SmartTodoResponse` now includes:

- bucket/status: `todoStatus`
- explanation: `primaryReasonCode`, `reasonCodes`, `reasonMessages`
- ranking support: `urgencyScore`, `scoreBreakdown`, `displayRank`, `recommendedFocus`
- daily progress: `currentProgress`, `targetProgress`, `remainingTodayTarget`, `progressUnit`, `progressDisplay`
- period pace: `periodCurrentProgress`, `periodTargetProgress`, `expectedProgressByToday`, `paceRatio`
- schedule context: `scheduleType`, `scheduleLabel`, `scheduleDetails`, `scheduledForToday`
- execution hint: `recommendedAction`, `suggestedTimeMinutes`

## What Changed vs Old Smart Todo

The old version mostly sorted by priority and a simple urgency score.

V1.1 adds:

- real bucketed prioritization
- schedule-aware explanations
- period pace tracking
- better completion logic
- recommended focus items
- frontend-friendly summary metadata

## Notes

- For `/date`, fields such as `scheduledForToday`, `completedToday`, and `expectedProgressByToday` should be interpreted as referring to the selected date.
- Future date views are planning-oriented. They do not simulate future activity completion.
- `CUSTOM` metrics are still less precise than `COUNT` and `DURATION` because the current activity payload does not carry custom quantity values.
- `suggestedTimeMinutes` now comes from remaining period time commitment across remaining actionable dates, then steps up when catch-up is needed.

## Companion Docs

- Backend implementation details: `docs/smart_todo_v1_1_implementation.md`
- Frontend integration guide: `docs/smart_todo_frontend_guide.md`
