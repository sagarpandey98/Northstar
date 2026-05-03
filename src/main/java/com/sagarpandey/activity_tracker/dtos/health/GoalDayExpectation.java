package com.sagarpandey.activity_tracker.dtos.health;

import java.time.LocalDate;

public class GoalDayExpectation {

    private LocalDate date;
    private boolean actionable;
    private Double expectedMinimumUnits = 0.0;
    private Double expectedTargetUnits = 0.0;

    public GoalDayExpectation() {}

    public GoalDayExpectation(
            LocalDate date,
            boolean actionable,
            Double expectedMinimumUnits,
            Double expectedTargetUnits) {
        this.date = date;
        this.actionable = actionable;
        this.expectedMinimumUnits = expectedMinimumUnits;
        this.expectedTargetUnits = expectedTargetUnits;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isActionable() {
        return actionable;
    }

    public void setActionable(boolean actionable) {
        this.actionable = actionable;
    }

    public Double getExpectedMinimumUnits() {
        return expectedMinimumUnits;
    }

    public void setExpectedMinimumUnits(Double expectedMinimumUnits) {
        this.expectedMinimumUnits = expectedMinimumUnits;
    }

    public Double getExpectedTargetUnits() {
        return expectedTargetUnits;
    }

    public void setExpectedTargetUnits(Double expectedTargetUnits) {
        this.expectedTargetUnits = expectedTargetUnits;
    }
}
