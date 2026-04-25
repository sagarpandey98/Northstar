# Goal Time-Bounded Ledger API & Payload Guide
**Version: 2.0 (Ledger Architecture)**

This document details the recent modifications to the Goal tracking system and explains how data should be mapped between the Frontend and Backend.

## 1. What Changed in the Refactor?
We completely overhauled the Goal Tracking engine to move away from rigid snapshot calculations (weekly snapshots) and towards a highly dynamic **Time-Bounded Ledger System**. 
The backend now maintains a ledger `GoalPeriod` which tracks exactly how well the user performed relative to the specific timeframe of the goal. 

**Legacy Fields Removed:**
The following fields were entirely purged from the `GoalRequest` and `GoalResponse` payload and should **no longer be sent** from the frontend (though they are safely ignored if you do send them):
- ❌ `targetFrequencyWeekly`
- ❌ `targetVolumeDaily`
- ❌ `scheduleDays` (replaced by strict JSON specs)
- ❌ `minimumSessionDaily`
- ❌ `evaluationPeriod` / `customPeriodDays`

**New Fields Added:**
- ✅ `scheduleSpec`: A structured JSON ruleset for highly flexible goal frequency constraints.
- ✅ `minimumSessionPeriod`: Time boundary for minimum desired effort.
- ✅ `maximumSessionPeriod`: Time boundary for maximum possible effort.

## 2. API Endpoints
All goal interactions occur through the standard REST controllers:

- `POST /api/v1/goals` - Create a new goal.
- `GET /api/v1/goals` - List all goals.
- `PUT /api/v1/goals/{id}` - Complete Goal override.
- `PATCH /api/v1/goals/{id}/progress` - Quickly add progress values.

## 3. Data Dictionary (`GoalRequest` & `GoalResponse`)

When sending data (`POST` or `PUT`) or parsing responses, use the following payload definitions:

### A) Core Goal Attributes
| Field Name | Type | Required | Description |
| :--- | :--- | :---: | :--- |
| `title` | String | Yes | Name of the goal. |
| `description` | String | No | Detailed outline of the goal. |
| `goalType` | Enum | No | `HABIT`, `PROJECT`, `SKILL`, `FITNESS`, `GENERAL`. Auto-populates defaults. |
| `priority` | Enum | No | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Defines sorting. |
| `startDate` | ISO Date | No | Used as the boundary starting line for the Ledger. Defaults to today. |
| `targetDate` | ISO Date | No | Must be after `startDate` if provided. |

### B) The New Bounded Ledger Properties (Crucial)
These fields entirely replace the old `scheduleDays` and `evaluationPeriod` logics.

| Field Name | Type | Required | Description |
| :--- | :--- | :---: | :--- |
| `scheduleSpec` | JSON Object | No | Replaces `scheduleDays` and `targetFrequencyWeekly`. Uses the V2 `scheduleType` + recursive `rules` contract. See schema below. |
| `minimumSessionPeriod` | Integer | No | The absolute minimum minutes required over the period to be deemed "consistent". (e.g. 120 means at least 2 hours of gym/week). HeavILY factors into Consistency Score. |
| `maximumSessionPeriod` | Integer | No | The ultimate time-limit target over the period (e.g. 300 minutes). Factors heavily into Progress Score limits. |
| `allowDoubleLogging` | Boolean | No | Indicates whether this goal allows duplicate log entries on the same day. Default `true`. |
| `missesAllowedPerPeriod`| Integer | No | Defines the "grace period" bounds (e.g., 2 free misses before the momentum breaks). |

### C) The `scheduleSpec` JSON Structure
The `scheduleSpec` is a strictly typed JSON object sent as-is. It supports simple schedules and detailed drill-down schedules.

```json
{
  "version": 2,
  "scheduleType": "WEEKLY",
  "timezone": "UTC",
  "weekStartsOn": "MONDAY",
  "requirements": {
    "minCheckins": 3,
    "maxCheckins": 5
  },
  "rules": [
    {
      "scope": "DAY_OF_WEEK",
      "values": ["MONDAY", "WEDNESDAY", "FRIDAY"],
      "mode": "STRICT"
    }
  ],
  "exclusions": [
    { "type": "DAY_OF_WEEK", "value": "SATURDAY" },
    { "type": "DAY_OF_WEEK", "value": "SUNDAY" }
  ]
}
```

For the full schema, drill-down rules, and examples such as yearly -> quarter -> month -> week -> day -> time, see `docs/schedule_spec_system_design.md`.

### D) Health Engine Weights
The user can optionally pass custom health calculation weights. The Bounded Ledger strictly requires these three to sum up to exactly `100` if provided. If not provided, they fall back to the `GoalType` defaults automatically.
| Field Name | Type | Rules |
| :--- | :--- | :--- |
| `consistencyWeight` | Integer | Must be 1-100. Measures meeting the `minimumSessionPeriod`. |
| `momentumWeight` | Integer | Must be 1-100. Measures streak longevity relative to previous periods. |
| `progressWeight` | Integer | Must be 1-100. Measures total volume vs `maximumSessionPeriod`. |

## 4. Frontend Integration Example
### Creating a New Goal
Instead of passing `targetFrequencyWeekly`, the frontend will now build the UI to pack the `scheduleSpec` and bounds. Because we added `@JsonIgnoreProperties`, the backend will silently ignore old frontend variables without breaking, but migrating to this new structure is vital.

**Incoming Payload (POST /api/v1/goals)**
```json
{
  "title": "Master Python",
  "priority": "HIGH",
  "metric": "DURATION",
  "targetOperator": "GREATER_THAN",
  "targetValue": 60,
  "goalType": "SKILL",
  
  "minimumSessionPeriod": 120,    
  "maximumSessionPeriod": 600,   
  
  "scheduleSpec": {
    "version": 2,
    "scheduleType": "WEEKLY",
    "timezone": "Asia/Kolkata",
    "requirements": {
      "minCheckins": 4
    },
    "rules": [
      {
        "scope": "DAY_OF_WEEK",
        "values": ["MONDAY", "WEDNESDAY", "FRIDAY"],
        "mode": "STRICT"
      }
    ]
  }
}
```

## 5. Smart Todo API

Smart Todo V1.1 now returns a wrapper object instead of a bare array.

### Endpoints

- `GET /api/v1/todos/today`
- `GET /api/v1/todos/date?date=YYYY-MM-DD`
- `POST /api/v1/todos/refresh`

### Response Shape

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
  "items": [
    {
      "goalId": 12,
      "title": "Morning Run",
      "priorityDisplay": "P1",
      "todoStatus": "MUST_DO_TODAY",
      "scheduledForToday": true,
      "scheduleType": "WEEKLY",
      "scheduleLabel": "WEEKLY • FRI • 3 check-ins / period",
      "currentProgress": 0,
      "targetProgress": 1,
      "remainingTodayTarget": 1,
      "progressUnit": "check-ins",
      "progressDisplay": "0 / 1 check-ins",
      "periodCurrentProgress": 1,
      "periodTargetProgress": 3,
      "expectedProgressByToday": 2,
      "paceRatio": 0.5,
      "reasonCodes": ["SCHEDULED_TODAY"],
      "reasonMessages": ["Scheduled for this date via FRI"],
      "recommendedAction": "Log 1 more check-in.",
      "suggestedTimeMinutes": 40,
      "recommendedFocus": true,
      "displayRank": 1
    }
  ]
}
```

### Item Buckets

- `MUST_DO_TODAY`
- `CATCH_UP_TODAY`
- `GOOD_TO_DO_TODAY`
- `COMPLETED_TODAY`

### Notes for Frontend

- `summary` should drive header counters and focus chips.
- `items` should be rendered grouped by `todoStatus`.
- For `/date`, fields that mention `today` should be interpreted as referring to the selected date.

For the deeper integration guide, see `docs/smart_todo_frontend_guide.md`.
