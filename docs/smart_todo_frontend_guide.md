# Smart Todo Frontend Guide

## Endpoints

Use these endpoints:

```text
GET  /api/v1/todos/today
GET  /api/v1/todos/date?date=YYYY-MM-DD
POST /api/v1/todos/refresh
```

All three now return the same wrapper:

```json
{
  "date": "2026-04-25",
  "timezone": "Asia/Kolkata",
  "listType": "TODAY",
  "summary": {},
  "items": []
}
```

## Recommended Frontend Mental Model

Do **not** render Smart Todo as one flat list sorted only by score.

Render it as sections:

1. `MUST_DO_TODAY`
2. `CATCH_UP_TODAY`
3. `GOOD_TO_DO_TODAY`
4. `COMPLETED_TODAY`

Within a section, use backend order directly via `displayRank`.

## Summary Usage

Use `summary` for top-level UI:

- counters for each bucket
- “Top 3 focus” chips from `recommendedFocusGoalIds` or `recommendedFocusTitles`
- empty-state logic

Recommended header pattern:

- title: `Smart Todo`
- subtitle: evaluated date in `timezone`
- small counts row from summary

## Item Rendering

Each item already contains enough context for a high-signal card.

### Show prominently

- `title`
- `todoStatus`
- `progressDisplay`
- `scheduleLabel`
- `recommendedAction`
- `reasonMessages[0]`

### Show as secondary context

- `suggestedTimeMinutes`
- `currentStreak`
- `lastCompletedDate`
- `periodProgressPercentage`
- `paceRatio`

### Use these for badges/chips

- `reasonCodes`
- `scheduledForToday`
- `streakAtRisk`
- `isBehindSchedule`
- `recommendedFocus`

## Suggested Card Layout

Top row:

- title
- priority chip (`priorityDisplay`)
- focus marker if `recommendedFocus = true`

Middle row:

- primary reason text from `reasonMessages[0]`
- progress line from `progressDisplay`

Bottom row:

- schedule chip from `scheduleLabel`
- time chip from `suggestedTimeMinutes`
- quick log CTA

Expandable details:

- full `reasonMessages`
- `scoreBreakdown`
- period progress

## Quick Log / Completion Flow

Use these fields to power the CTA:

- `requiresQuickLog`
- `quickLogContext`
- `recommendedAction`
- `remainingTodayTarget`

Recommended CTA rules:

- if `COMPLETED_TODAY`, show muted done state
- if `remainingTodayTarget > 0`, show primary action button
- if `suggestedTimeMinutes` exists, include it in tooltip/subcopy

## How To Use `reasonCodes`

Use `reasonCodes` for styling and stable UI logic.

Examples:

- `SCHEDULED_TODAY` -> calendar icon / due chip
- `STREAK_AT_RISK` -> warning styling
- `BEHIND_PACE` -> catch-up badge
- `DEADLINE_NEAR` -> deadline chip
- `TODAY_TARGET_MET` -> completed styling

Do not parse `reasonMessages` for logic.

## How To Use `scoreBreakdown`

This is ideal for:

- debug drawers
- tooltip explanations
- “Why is this ranked here?” surfaces

Avoid making the raw score the main UI. The bucket is more legible.

## `/today` vs `/date`

### `/today`

Use for the default dashboard and home screen.

### `/date`

Use for:

- calendar day drill-down
- retrospective review
- future planning

Important note:

Fields like `scheduledForToday`, `completedToday`, and `expectedProgressByToday` should be interpreted relative to the selected date from the response payload.

## Recommended Empty States

### No items

Show a calm empty state:

- `Nothing urgent for this date`
- optional secondary text from summary counts

### Only completed items

Show completed section expanded by default with subtle celebration, not a full-page empty state.

## Recommended Frontend Enhancements

1. default collapse `COMPLETED_TODAY`
2. pin the first 3 `recommendedFocus` items near the top
3. allow quick filter by reason code
4. let the user switch between `Today` and `Selected Date`
5. show score breakdown only on demand

## Migration Note

Old frontend code that expects `GET /todos/today` to return an array must be updated to read:

- `response.items`
- `response.summary`

Do not flatten the wrapper away in your state layer; the summary is useful.
