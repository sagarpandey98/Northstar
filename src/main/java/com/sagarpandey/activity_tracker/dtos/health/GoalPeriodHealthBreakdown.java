package com.sagarpandey.activity_tracker.dtos.health;

import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.Goal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GoalPeriodHealthBreakdown {

    private Long goalId;
    private String goalUuid;
    private String periodUuid;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate evaluationDate;
    private Goal.Metric metric;
    private String unitLabel;

    private Double consistencyScore;
    private Double momentumScore;
    private Double progressScore;
    private Double healthScore;
    private HealthStatus healthStatus;

    private Integer consistencyWeight;
    private Integer momentumWeight;
    private Integer progressWeight;

    private Integer actionableDayCount;
    private Integer actionableDayCountToDate;
    private Double totalExpectedMinimumUnits;
    private Double totalExpectedTargetUnits;
    private Double expectedMinimumUnitsToDate;
    private Double expectedTargetUnitsToDate;
    private Double actualUnits;
    private Double actualUnitsToDate;

    private MomentumBreakdown momentumBreakdown;
    private List<GoalDayHealthDetail> dailyDetails = new ArrayList<>();

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

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

    public Goal.Metric getMetric() {
        return metric;
    }

    public void setMetric(Goal.Metric metric) {
        this.metric = metric;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String unitLabel) {
        this.unitLabel = unitLabel;
    }

    public Double getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(Double consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public Double getMomentumScore() {
        return momentumScore;
    }

    public void setMomentumScore(Double momentumScore) {
        this.momentumScore = momentumScore;
    }

    public Double getProgressScore() {
        return progressScore;
    }

    public void setProgressScore(Double progressScore) {
        this.progressScore = progressScore;
    }

    public Double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Double healthScore) {
        this.healthScore = healthScore;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Integer getConsistencyWeight() {
        return consistencyWeight;
    }

    public void setConsistencyWeight(Integer consistencyWeight) {
        this.consistencyWeight = consistencyWeight;
    }

    public Integer getMomentumWeight() {
        return momentumWeight;
    }

    public void setMomentumWeight(Integer momentumWeight) {
        this.momentumWeight = momentumWeight;
    }

    public Integer getProgressWeight() {
        return progressWeight;
    }

    public void setProgressWeight(Integer progressWeight) {
        this.progressWeight = progressWeight;
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

    public Double getActualUnits() {
        return actualUnits;
    }

    public void setActualUnits(Double actualUnits) {
        this.actualUnits = actualUnits;
    }

    public Double getActualUnitsToDate() {
        return actualUnitsToDate;
    }

    public void setActualUnitsToDate(Double actualUnitsToDate) {
        this.actualUnitsToDate = actualUnitsToDate;
    }

    public MomentumBreakdown getMomentumBreakdown() {
        return momentumBreakdown;
    }

    public void setMomentumBreakdown(MomentumBreakdown momentumBreakdown) {
        this.momentumBreakdown = momentumBreakdown;
    }

    public List<GoalDayHealthDetail> getDailyDetails() {
        return dailyDetails;
    }

    public void setDailyDetails(List<GoalDayHealthDetail> dailyDetails) {
        this.dailyDetails = dailyDetails;
    }
}
