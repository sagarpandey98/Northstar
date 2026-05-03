package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import java.time.LocalDate;

public class GoalPeriodCreateRequest {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Double currentValue;
    private ScheduleSpec scheduleSpec;

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    public ScheduleSpec getScheduleSpec() { return scheduleSpec; }
    public void setScheduleSpec(ScheduleSpec scheduleSpec) { this.scheduleSpec = scheduleSpec; }
}
