package com.sagarpandey.activity_tracker.enums;

/**
 * Distinguishes a real logged activity from a deliberate "No activity" / skip log.
 *
 * ACTIVITY — a normal completion; counts toward goal progress, smart-todo progress,
 *            and the health engine (consistency / momentum / progress / streaks).
 * SKIP     — a "No activity" record explaining WHY the activity was not done. It is
 *            never counted anywhere; it exists purely for visibility and later analysis.
 *
 * A null entryType on legacy rows is treated as ACTIVITY everywhere.
 */
public enum EntryType {
    ACTIVITY,
    SKIP
}
