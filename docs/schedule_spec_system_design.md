# Schedule Spec System Design

## 1. Purpose

`schedule_spec` is the contract that answers:

> When is activity for this goal expected or allowed?

It should support simple schedules like "daily" and deeply nested schedules like:

> Yearly goal -> Q1 and Q4 -> Q1/M1 -> W1 on MWF, W2 flexible, W3 Monday at 09:00 and 21:00.

This document proposes `schedule_spec` version 2. The goal is to make the format:

- expressive enough for detailed goal scheduling
- readable by humans
- deterministic for backend evaluation
- safe to snapshot into `goal_periods`
- extensible for future rule types

## 2. Core Concepts

### Schedule Type

Every scheduled goal starts with one root schedule type:

```text
DAILY
WEEKLY
MONTHLY
QUARTERLY
YEARLY
```

This controls the primary evaluation period for the goal. For example:

- `WEEKLY` means the goal is evaluated week by week.
- `MONTHLY` means the goal is evaluated month by month.
- `YEARLY` means the goal is evaluated year by year.

The selected `scheduleType` does not need to describe every detail. Details are captured by the rule tree.

### Rule Tree

The rule tree lets the user drill down into the selected schedule type.

Sibling rules are combined with OR.

Parent-to-child rules are combined with AND.

For example:

```text
MONTHLY
  W1 -> MONDAY, WEDNESDAY, FRIDAY
  W3 -> FLEXIBLE
```

Means:

```text
Allowed in W1 only on Monday, Wednesday, Friday
OR
Allowed anywhere inside W3
```

### Strict vs Flexible

Every rule has a `mode`.

| Mode | Meaning |
|---|---|
| `STRICT` | The selected scope is restricted further by child rules, or by the rule itself if it is a leaf. |
| `FLEXIBLE` | The selected scope is a terminal flexible allowance. Any date/time inside this selected scope is allowed. |

Important:

`FLEXIBLE` always means flexible inside the current scope, not globally.

Examples:

- `Q4 FLEXIBLE` means any date/time inside Q4.
- `M3 FLEXIBLE` under `Q1` means any date/time inside month 3 of Q1.
- `W2 FLEXIBLE` under `M1` means any date/time inside week 2 of that month.

A `FLEXIBLE` rule should not have child rules. If the user wants drill-down, use `STRICT`.

## 3. Top-Level JSON Shape

```json
{
  "version": 2,
  "scheduleType": "MONTHLY",
  "timezone": "Asia/Kolkata",
  "weekStartsOn": "MONDAY",
  "weekOfMonthModel": "DAY_BUCKETS",
  "requirements": {
    "minCheckins": 0,
    "maxCheckins": null
  },
  "rules": [],
  "exclusions": []
}
```

### Top-Level Fields

| Field | Required | Meaning |
|---|---:|---|
| `version` | Yes | Schema version. This proposal uses `2`. |
| `scheduleType` | Yes | Root schedule period: `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY`. |
| `timezone` | Yes | IANA timezone used for all date/time evaluation. Example: `Asia/Kolkata`. |
| `weekStartsOn` | No | Used for weekly period boundaries. Default: `MONDAY`. |
| `weekOfMonthModel` | No | How `W1`, `W2`, etc. are interpreted. Default: `DAY_BUCKETS`. |
| `requirements` | No | Optional check-in count requirements for the root period. |
| `rules` | No | Optional rule tree. Empty means flexible across the whole root period. |
| `exclusions` | No | Veto rules that remove otherwise allowed dates/times. |

## 4. Rule Node Shape

```json
{
  "scope": "WEEK_OF_MONTH",
  "values": [1],
  "mode": "STRICT",
  "requirements": {
    "minCheckins": 1,
    "maxCheckins": null
  },
  "rules": []
}
```

### Rule Fields

| Field | Required | Meaning |
|---|---:|---|
| `scope` | Yes | The calendar scope this rule selects. |
| `values` | Usually | Selected values for the scope. |
| `mode` | Yes | `STRICT` or `FLEXIBLE`. |
| `requirements` | No | Optional check-in count requirement inside this rule's scope. |
| `rules` | No | Child rule nodes for deeper drill-down. |

## 5. Supported Scopes

### QUARTER

Selects quarters inside a yearly goal.

```json
{ "scope": "QUARTER", "values": [1, 4], "mode": "FLEXIBLE" }
```

Accepted values:

```text
1, 2, 3, 4
```

### MONTH_OF_YEAR

Selects calendar months inside a yearly goal.

```json
{ "scope": "MONTH_OF_YEAR", "values": [1, 3, 12], "mode": "FLEXIBLE" }
```

Accepted values:

```text
1 through 12
```

`1` means January. `12` means December.

### MONTH_OF_QUARTER

Selects the first, second, or third month inside a quarter.

```json
{ "scope": "MONTH_OF_QUARTER", "values": [1, 3], "mode": "FLEXIBLE" }
```

Accepted values:

```text
1, 2, 3
```

Example:

- Under Q1, `MONTH_OF_QUARTER = 1` means January.
- Under Q1, `MONTH_OF_QUARTER = 3` means March.
- Under Q4, `MONTH_OF_QUARTER = 1` means October.
- Under Q4, `MONTH_OF_QUARTER = 3` means December.

### WEEK_OF_MONTH

Selects week buckets inside a month.

```json
{ "scope": "WEEK_OF_MONTH", "values": [1, 3], "mode": "FLEXIBLE" }
```

Accepted values:

```text
1, 2, 3, 4, 5
```

With `weekOfMonthModel = DAY_BUCKETS`:

| Value | Date Range |
|---:|---|
| `1` | Day 1 through 7 |
| `2` | Day 8 through 14 |
| `3` | Day 15 through 21 |
| `4` | Day 22 through 28 |
| `5` | Day 29 through end of month |

This model is simple, stable, and easy for users to understand. A future `CALENDAR_WEEKS` model can be added if the product needs Monday-Sunday calendar week behavior inside months.

### DAY_OF_MONTH

Selects fixed dates inside a month.

```json
{ "scope": "DAY_OF_MONTH", "values": [10, 25, 28], "mode": "STRICT" }
```

Accepted values:

```text
1 through 31, "LAST"
```

If a selected date does not exist in a month, it is skipped for that month. Use `"LAST"` to mean the actual last day of the month.

### DAY_OF_WEEK

Selects weekdays.

```json
{
  "scope": "DAY_OF_WEEK",
  "values": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "mode": "STRICT"
}
```

Accepted values:

```text
MONDAY
TUESDAY
WEDNESDAY
THURSDAY
FRIDAY
SATURDAY
SUNDAY
```

### TIME_OF_DAY

Selects exact planned times inside a selected date.

```json
{
  "scope": "TIME_OF_DAY",
  "values": ["09:00", "21:00"],
  "mode": "STRICT"
}
```

Accepted values use 24-hour `HH:mm` format.

`TIME_OF_DAY` is best for planned check-in slots like 09:00 and 21:00. If the user wants a range, use `TIME_WINDOW`.

### TIME_WINDOW

Selects time ranges inside a selected date.

```json
{
  "scope": "TIME_WINDOW",
  "windows": [
    { "start": "07:00", "end": "09:00" },
    { "start": "18:00", "end": "20:00" }
  ],
  "mode": "STRICT"
}
```

Accepted values use 24-hour `HH:mm` format. `TIME_WINDOW` rules use `windows` instead of `values`.

## 6. Requirements

`requirements` describes how many check-ins are expected inside a period or rule scope.

```json
{
  "minCheckins": 3,
  "maxCheckins": 5
}
```

Rules:

- Top-level requirements apply to the whole root period.
- Rule-level requirements apply inside that rule's selected scope.
- `minCheckins` means the minimum check-ins needed to satisfy that scope.
- `maxCheckins` means the maximum check-ins counted or allowed in that scope.
- If both top-level and rule-level requirements exist, both can be evaluated.

Important separation:

`schedule_spec` should not replace goal progress fields like `targetValue`, `minimumSessionPeriod`, or `maximumSessionPeriod`.

Use `requirements` for check-in frequency. Use goal/period fields for count, duration, custom metric, and health score targets.

## 7. Exclusions

Exclusions are veto rules. They remove dates/times that would otherwise be allowed by the rule tree.

```json
{
  "exclusions": [
    { "type": "DATE", "value": "2026-12-25" },
    { "type": "DATE_RANGE", "start": "2026-12-24", "end": "2026-12-31" },
    { "type": "DAY_OF_WEEK", "value": "SUNDAY" },
    { "type": "MONTH_OF_YEAR", "value": 12 }
  ]
}
```

Supported exclusion types:

| Type | Meaning |
|---|---|
| `DATE` | One absolute date in `YYYY-MM-DD` format. |
| `DATE_RANGE` | Inclusive absolute date range. |
| `DAY_OF_WEEK` | Exclude every matching weekday. |
| `MONTH_OF_YEAR` | Exclude every matching calendar month. |

## 8. Evaluation Semantics

Given a local date/time in the schedule timezone:

1. Determine the active root period from `scheduleType`.
2. Apply `exclusions`. If any exclusion matches, the date/time is not actionable.
3. If `rules` is empty, the date/time is actionable anywhere inside the root period.
4. If `rules` is present, at least one root rule branch must match.
5. For siblings, matching is OR.
6. For parent-to-child rules, matching is AND.
7. A `FLEXIBLE` node matches any date/time inside that node's selected scope and stops traversal.
8. A `STRICT` node with children must match at least one child.
9. A `STRICT` leaf node matches only its selected scope.

### Scope Skipping

Intermediate scopes may be skipped when the meaning is unambiguous. Omitted scopes mean "all values at that level."

Examples:

- `YEARLY -> DAY_OF_WEEK MONDAY` means every Monday in the year.
- `MONTHLY -> DAY_OF_WEEK MONDAY` means every Monday in the month.
- `QUARTERLY -> WEEK_OF_MONTH 1` means week 1 of every month in the quarter.

Even with scope skipping, rule scopes must always move from broader to narrower time units.

## 9. Examples

### Example A: Monthly Fixed Dates

User says:

> Every month, I want to do this goal on the 10th, 25th, and 28th.

```json
{
  "version": 2,
  "scheduleType": "MONTHLY",
  "timezone": "Asia/Kolkata",
  "rules": [
    {
      "scope": "DAY_OF_MONTH",
      "values": [10, 25, 28],
      "mode": "STRICT"
    }
  ]
}
```

### Example B: Monthly Week Drill-Down

User says:

> Monthly goal. In W1 I will do Monday, Wednesday, Friday. In W3 I am flexible.

```json
{
  "version": 2,
  "scheduleType": "MONTHLY",
  "timezone": "Asia/Kolkata",
  "weekOfMonthModel": "DAY_BUCKETS",
  "rules": [
    {
      "scope": "WEEK_OF_MONTH",
      "values": [1],
      "mode": "STRICT",
      "rules": [
        {
          "scope": "DAY_OF_WEEK",
          "values": ["MONDAY", "WEDNESDAY", "FRIDAY"],
          "mode": "STRICT"
        }
      ]
    },
    {
      "scope": "WEEK_OF_MONTH",
      "values": [3],
      "mode": "FLEXIBLE"
    }
  ]
}
```

Meaning:

- W1 is allowed only on Monday, Wednesday, Friday.
- W3 is allowed on any day inside W3.
- Other weeks are not allowed unless another rule includes them.

### Example C: Weekly Goal on Selected Days

User says:

> Weekly goal. I want to do it on Monday, Wednesday, Friday.

```json
{
  "version": 2,
  "scheduleType": "WEEKLY",
  "timezone": "Asia/Kolkata",
  "requirements": {
    "minCheckins": 3
  },
  "rules": [
    {
      "scope": "DAY_OF_WEEK",
      "values": ["MONDAY", "WEDNESDAY", "FRIDAY"],
      "mode": "STRICT"
    }
  ]
}
```

### Example D: Yearly Goal with Deep Drill-Down

User says:

> Yearly goal. I will do goal activity in Q1 and Q4.
> For Q1, I want M1 and M3.
> For M1, W1 is MWF, W2 is flexible, and W3 is Monday at 09:00 and 21:00.

```json
{
  "version": 2,
  "scheduleType": "YEARLY",
  "timezone": "Asia/Kolkata",
  "weekOfMonthModel": "DAY_BUCKETS",
  "rules": [
    {
      "scope": "QUARTER",
      "values": [1],
      "mode": "STRICT",
      "rules": [
        {
          "scope": "MONTH_OF_QUARTER",
          "values": [1],
          "mode": "STRICT",
          "rules": [
            {
              "scope": "WEEK_OF_MONTH",
              "values": [1],
              "mode": "STRICT",
              "rules": [
                {
                  "scope": "DAY_OF_WEEK",
                  "values": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                  "mode": "STRICT"
                }
              ]
            },
            {
              "scope": "WEEK_OF_MONTH",
              "values": [2],
              "mode": "FLEXIBLE"
            },
            {
              "scope": "WEEK_OF_MONTH",
              "values": [3],
              "mode": "STRICT",
              "rules": [
                {
                  "scope": "DAY_OF_WEEK",
                  "values": ["MONDAY"],
                  "mode": "STRICT",
                  "rules": [
                    {
                      "scope": "TIME_OF_DAY",
                      "values": ["09:00", "21:00"],
                      "mode": "STRICT"
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          "scope": "MONTH_OF_QUARTER",
          "values": [3],
          "mode": "FLEXIBLE"
        }
      ]
    },
    {
      "scope": "QUARTER",
      "values": [4],
      "mode": "FLEXIBLE"
    }
  ]
}
```

Meaning:

- Q1 is restricted by child rules.
- In Q1, month 1 is restricted further.
- In Q1 month 1:
  - W1 allows Monday, Wednesday, Friday.
  - W2 allows any day/time inside W2.
  - W3 allows only Monday at 09:00 and 21:00.
- In Q1, month 3 is fully flexible.
- Q4 is fully flexible.
- Q2 and Q3 are not allowed unless additional rules are added.

### Example E: Daily Goal with Time Windows

User says:

> Daily goal. I want to do it either in the morning or evening.

```json
{
  "version": 2,
  "scheduleType": "DAILY",
  "timezone": "Asia/Kolkata",
  "rules": [
    {
      "scope": "TIME_WINDOW",
      "windows": [
        { "start": "07:00", "end": "09:00" },
        { "start": "18:00", "end": "20:00" }
      ],
      "mode": "STRICT"
    }
  ]
}
```

## 10. Validation Rules

The backend should validate `schedule_spec` before saving a goal.

Required validations:

- `version` must be supported.
- `scheduleType` must be one of the supported root types.
- `timezone` must be a valid IANA timezone.
- Every rule must have a valid `scope`.
- Every rule must have a valid `mode`.
- `FLEXIBLE` rules must not contain child rules.
- Rule scopes must move from broader to narrower time units.
- Values must match the rule scope.
- `TIME_OF_DAY` values must be valid `HH:mm`.
- `TIME_WINDOW` entries must have valid `start` and `end`.
- `minCheckins` and `maxCheckins` must be non-negative if present.
- `maxCheckins` must be greater than or equal to `minCheckins` if both are present.

Recommended validations:

- Reject duplicate sibling rules with the same scope, values, and mode.
- Reject empty `values` unless the scope type explicitly allows no values.
- Warn if a selected day of month never exists for some months, such as `31`.
- Warn if a strict container node has no child rules and is not a leaf-like scope.

## 11. Storage Rules

### Goal

The current goal stores the user's latest intended schedule.

### Goal Period

Each `GoalPeriod` should store a snapshot of the `schedule_spec` that was active when the period was created.

Historical periods should not be changed automatically when the user updates the goal's schedule. New periods should use the latest goal schedule.

This lets reports and health scoring stay historically correct.

### Database Type

Preferred storage is `JSONB` when using PostgreSQL.

If the application stores the field as `TEXT`, the backend must still treat it as a strict JSON contract and validate it before saving.

## 12. Engine Responsibilities

The schedule engine should expose deterministic operations:

```text
validate(spec)
getPeriodRange(spec, referenceDate)
isActionable(spec, dateTime)
countActionableDays(spec, periodStart, periodEnd)
listActionableDates(spec, periodStart, periodEnd)
listPlannedTimeSlots(spec, date)
```

Smart Todo should depend on this engine instead of reimplementing schedule logic.

## 13. Boundaries and Future Extensions

This design handles most structured recurring schedules needed for goal tracking:

- daily schedules
- weekly schedules
- monthly fixed dates
- monthly week drill-down
- quarterly month drill-down
- yearly quarter/month drill-down
- selected days of week
- exact planned times
- time windows
- exclusions
- flexible scopes

Some advanced cases should be future extensions:

- every N days
- every third business day
- last Friday of the month
- first weekday after salary day
- alternate Mondays
- public holiday calendars
- external calendar imports
- conditional schedules like "if I missed yesterday"

These should be added as explicit new scopes or rule types, not by overloading existing fields.

## 14. Design Verdict

This V2 design can support the detailed drill-down scheduling model:

```text
YEARLY -> QUARTER -> MONTH_OF_QUARTER -> WEEK_OF_MONTH -> DAY_OF_WEEK -> TIME
```

It can also support simpler schedules without forcing deeply nested JSON.

The most important rules are:

- `scheduleType` defines the root evaluation period.
- `rules` define the drill-down calendar structure.
- `STRICT` means keep drilling down or enforce the selected leaf.
- `FLEXIBLE` means any date/time inside the selected scope.
- `exclusions` always veto.
- `requirements` describe check-in frequency, not progress volume.
