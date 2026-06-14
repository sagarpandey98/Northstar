package com.sagarpandey.activity_tracker.dtos.health;

import java.time.LocalDate;

public class GoalDayHealthDetail {

    private LocalDate date;
    private boolean actionable;
    private boolean countedInScore;
    private Double expectedMinimumUnits;
    private Double expectedTargetUnits;
    private Double actualUnits;
    private Double consistencyFulfilledUnits;
    private Double progressFulfilledUnits;
    private Double consistencyScore;
    private Double progressScore;

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

    public boolean isCountedInScore() {
        return countedInScore;
    }

    public void setCountedInScore(boolean countedInScore) {
        this.countedInScore = countedInScore;
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

    public Double getActualUnits() {
        return actualUnits;
    }

    public void setActualUnits(Double actualUnits) {
        this.actualUnits = actualUnits;
    }

    public Double getConsistencyFulfilledUnits() {
        return consistencyFulfilledUnits;
    }

    public void setConsistencyFulfilledUnits(Double consistencyFulfilledUnits) {
        this.consistencyFulfilledUnits = consistencyFulfilledUnits;
    }

    public Double getProgressFulfilledUnits() {
        return progressFulfilledUnits;
    }

    public void setProgressFulfilledUnits(Double progressFulfilledUnits) {
        this.progressFulfilledUnits = progressFulfilledUnits;
    }

    public Double getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(Double consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public Double getProgressScore() {
        return progressScore;
    }

    public void setProgressScore(Double progressScore) {
        this.progressScore = progressScore;
    }
}
