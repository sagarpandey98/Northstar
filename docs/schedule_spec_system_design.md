# 📘 Schedule Spec — System Design Document

## 🧠 Overview
`schedule_spec` defines a hierarchical rule engine used to control when a user can perform check-ins for a goal. 
It is a recursive time-constraint tree built using fixed frequency levels and strict value enums, designed to be stored natively as a `JSONB` object within the `goals` database table.

## 🏗️ Top-Level Structure

```json
{
  "frequency": "<ROOT_FREQUENCY>",
  "flexible": "<boolean>",
  "timezone": "<string>", // e.g., "Asia/Kolkata" or "UTC"
  "constraints": {
    "minCheckinsRequired": "<number>",
    "maxCheckinsAllowed": "<number>"
  },
  "exclusions": [ "<EXCLUSION_RULES>" ],
  "segments": [ "<SEGMENT_TREE>" ]
}
```

### 📌 1. `frequency` (MANDATORY)
Defines the scope level of the rule system.

| Value | Purpose |
|---|---|
| `YEARLY` | Year-based grouping |
| `QUARTERLY` | Quarter-based grouping |
| `MONTHLY` | Monthly goal window |
| `WEEKLY` | Weekly cycle |
| `DAILY` | Day-level scope |
| `TIMING` | Exact time slots |

### 📌 2. `flexible` (BOOLEAN)
Defines whether the root level is strict or relaxed.

| Value | Meaning |
|---|---|
| `true` | User can deviate within constraints. |
| `false` | Strict rule enforcement. |

### 📌 3. `timezone` (OPTIONAL / RECOMMENDED)
Essential for correctly evaluating `TIMING` constraints.
- If omitted, defaults to the user's globally saved timezone or UTC.

### 📌 4. `constraints` (OPTIONAL)
Defines global execution limits.
```json
{
  "minCheckinsRequired": 0,
  "maxCheckinsAllowed": 0
}
```

### 📌 5. `segments` (RECURSIVE TREE)
Defines hierarchical breakdown of schedule rules.
Each segment follows the same structure. 
**Note:** `values` takes an Array `[]` to prevent bloated JSON objects when selecting multiple days or times.

```json
{
  "frequency": "<LEVEL>",
  "values": ["<ENUM_VALUE_1>", "<ENUM_VALUE_2>"],
  "flexible": "<boolean>",
  "segments": [ "optional nested segments" ]
}
```

### 📌 6. `exclusions` (OPTIONAL)
Provides top-level logic to explicitly override and skip dates/times that the `segments` tree would otherwise allow. This acts as a veto rule, perfect for preventing schedules from triggering on vacations or public holidays.

```json
"exclusions": [
  { "type": "DATE", "value": "2026-12-25" },
  { "type": "DAY_OF_WEEK", "value": "SUNDAY" }
]
```

| Type | Accepted Values |
|---|---|
| `DATE` | Specific absolute date in `YYYY-MM-DD` format |
| `DAY_OF_WEEK` | `MONDAY` ... `SUNDAY` |
| `SPECIFIC_MONTH` | `JANUARY` ... `DECEMBER` |

---

## 🌳 Hierarchy Model & Mapping Rules
Each level reduces the time scope. You can safely skip intermediate levels (e.g., `MONTHLY` -> `DAILY`).

### 1. YEARLY
- **Valid Children:** `MONTHLY`
- **Values Array Accepts:** `JANUARY`, `FEBRUARY`, ... `DECEMBER`

### 2. QUARTERLY
- **Valid Children:** `MONTHLY`
- **Values Array Accepts:** `M1`, `M2`, `M3` (Months of the quarter)

### 3. MONTHLY
- **Valid Children:** `WEEKLY`, `DAILY`
- **Values Array Accepts:** `W1`, `W2`, `W3`, `W4`, `W5`

### 4. WEEKLY
- **Valid Children:** `DAILY`
- **Values Array Accepts:** `MONDAY`, `TUESDAY` ... `SUNDAY`

### 5. DAILY
- **Valid Children:** `TIMING`
- **Values Array Accepts:** 
  - If parent is `MONTHLY`, `QUARTERLY`, or `YEARLY`: Specific dates like `D1` to `D31`, `LAST_DAY_OF_MONTH`
  - If parent is `WEEKLY`: Days of the week like `MONDAY` ... `SUNDAY`

### 6. TIMING
- **Valid Children:** None (Leaf nodes only)
- **Values Array Accepts:** Exact strings `"00:00"` through `"23:59"`

---

## ⚙️ Flexibility Rules

| Level | `flexible = true` | `flexible = false` |
|---|---|---|
| **MONTHLY** | Relaxed month completion | Strict month constraint |
| **WEEKLY** | Optional week enforcement | Strict selected weeks |
| **DAILY** | Any day allowed in scope | Only selected days (or dates) |
| **TIMING** | Time window allowed | Exact time match strictly enforced |

---

## 🧾 FULL EXAMPLES

### Example 1: Standard Corporate Shift
*Every Monday to Friday, exactly at 09:00 and 18:00 (strict).*

```json
{
  "frequency": "WEEKLY",
  "flexible": false,
  "timezone": "America/New_York",
  "segments": [
    {
      "frequency": "DAILY",
      "values": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
      "flexible": false,
      "segments": [
        {
          "frequency": "TIMING",
          "values": ["09:00", "18:00"],
          "flexible": false
        }
      ]
    }
  ]
}
```

### Example 2: Pay Rent
*Strictly on the 1st of every Month.*

```json
{
  "frequency": "MONTHLY",
  "flexible": false,
  "segments": [
    {
      "frequency": "DAILY",
      "values": ["D1"],
      "flexible": false
    }
  ]
}
```

### Example 3: Mixed Rules
*Required to check in Week 1 mostly on Wednesday at specific times. But Week 2 can be flexible.*

```json
{
  "frequency": "MONTHLY",
  "flexible": false,
  "constraints": {
    "minCheckinsRequired": 5,
    "maxCheckinsAllowed": 20
  },
  "segments": [
    {
      "frequency": "WEEKLY",
      "values": ["W1"],
      "flexible": false,
      "segments": [
        {
          "frequency": "DAILY",
          "values": ["WEDNESDAY"],
          "flexible": false,
          "segments": [
            {
              "frequency": "TIMING",
              "values": ["09:00", "18:00"],
              "flexible": true
            }
          ]
        },
        {
          "frequency": "DAILY",
          "values": ["FRIDAY"],
          "flexible": true
        }
      ]
    },
    {
      "frequency": "WEEKLY",
      "values": ["W2"],
      "flexible": true
    }
  ]
}
```
