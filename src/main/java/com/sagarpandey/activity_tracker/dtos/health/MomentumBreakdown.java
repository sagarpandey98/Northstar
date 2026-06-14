package com.sagarpandey.activity_tracker.dtos.health;

public class MomentumBreakdown {

    private Double currentCompositeScore;
    private Double baselineCompositeScore;
    private Double deltaFromBaseline;
    private Integer periodsCompared;
    private String trend;
    private String explanation;

    public Double getCurrentCompositeScore() {
        return currentCompositeScore;
    }

    public void setCurrentCompositeScore(Double currentCompositeScore) {
        this.currentCompositeScore = currentCompositeScore;
    }

    public Double getBaselineCompositeScore() {
        return baselineCompositeScore;
    }

    public void setBaselineCompositeScore(Double baselineCompositeScore) {
        this.baselineCompositeScore = baselineCompositeScore;
    }

    public Double getDeltaFromBaseline() {
        return deltaFromBaseline;
    }

    public void setDeltaFromBaseline(Double deltaFromBaseline) {
        this.deltaFromBaseline = deltaFromBaseline;
    }

    public Integer getPeriodsCompared() {
        return periodsCompared;
    }

    public void setPeriodsCompared(Integer periodsCompared) {
        this.periodsCompared = periodsCompared;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
