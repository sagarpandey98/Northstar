package com.sagarpandey.activity_tracker.dtos.health;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GoalPeriodExpectation {

    private String goalUuid;
    private String periodUuid;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate evaluationDate;
    private Integer actionableDayCount = 0;
    private Integer actionableDayCountToDate = 0;
    private Double totalExpectedMinimumUnits = 0.0;
    private Double totalExpectedTargetUnits = 0.0;
    private Double expectedMinimumUnitsToDate = 0.0;
    private Double expectedTargetUnitsToDate = 0.0;
    private List<GoalDayExpectation> dailyExpectations = new ArrayList<>();

    public String getGoalUuid() {
        return goalUuid;
    }

    public void setGoalUuid(String goalUuid) {
        this.goalUuid = goalUuid;
    }

    public String getPeriodUuid() {
        return periodUuid;
    }

    public void setPeriodUuid(String periodUuid) {
        this.periodUuid = periodUuid;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public LocalDate getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(LocalDate evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public Integer getActionableDayCount() {
        return actionableDayCount;
    }

    public void setActionableDayCount(Integer actionableDayCount) {
        this.actionableDayCount = actionableDayCount;
    }

    public Integer getActionableDayCountToDate() {
        return actionableDayCountToDate;
    }

    public void setActionableDayCountToDate(Integer actionableDayCountToDate) {
        this.actionableDayCountToDate = actionableDayCountToDate;
    }

    public Double getTotalExpectedMinimumUnits() {
        return totalExpectedMinimumUnits;
    }

    public void setTotalExpectedMinimumUnits(Double totalExpectedMinimumUnits) {
        this.totalExpectedMinimumUnits = totalExpectedMinimumUnits;
    }

    public Double getTotalExpectedTargetUnits() {
        return totalExpectedTargetUnits;
    }

    public void setTotalExpectedTargetUnits(Double totalExpectedTargetUnits) {
        this.totalExpectedTargetUnits = totalExpectedTargetUnits;
    }

    public Double getExpectedMinimumUnitsToDate() {
        return expectedMinimumUnitsToDate;
    }

    public void setExpectedMinimumUnitsToDate(Double expectedMinimumUnitsToDate) {
        this.expectedMinimumUnitsToDate = expectedMinimumUnitsToDate;
    }

    public Double getExpectedTargetUnitsToDate() {
        return expectedTargetUnitsToDate;
    }

    public void setExpectedTargetUnitsToDate(Double expectedTargetUnitsToDate) {
        this.expectedTargetUnitsToDate = expectedTargetUnitsToDate;
    }

    public List<GoalDayExpectation> getDailyExpectations() {
        return dailyExpectations;
    }

    public void setDailyExpectations(List<GoalDayExpectation> dailyExpectations) {
        this.dailyExpectations = dailyExpectations;
    }
}
