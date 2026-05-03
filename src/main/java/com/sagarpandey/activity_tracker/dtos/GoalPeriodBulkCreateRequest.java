package com.sagarpandey.activity_tracker.dtos;

import java.time.LocalDate;

public class GoalPeriodBulkCreateRequest {

    private LocalDate startDate;
    private LocalDate throughDate;
    private Integer maxPeriods;
    private Boolean fillGaps = Boolean.TRUE;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getThroughDate() { return throughDate; }
    public void setThroughDate(LocalDate throughDate) { this.throughDate = throughDate; }
    public Integer getMaxPeriods() { return maxPeriods; }
    public void setMaxPeriods(Integer maxPeriods) { this.maxPeriods = maxPeriods; }
    public Boolean getFillGaps() { return fillGaps; }
    public void setFillGaps(Boolean fillGaps) { this.fillGaps = fillGaps; }
}
