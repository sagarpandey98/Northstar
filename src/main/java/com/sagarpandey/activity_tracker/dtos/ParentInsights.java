package com.sagarpandey.activity_tracker.dtos;

import com.sagarpandey.activity_tracker.enums.HealthStatus;

public class ParentInsights {

    // Distribution of children by health status
    private ChildrenSummary childrenSummary;

    // The child with the lowest healthScore
    // Null if no children have health scores yet
    private WeakestChild weakestChild;

    // Completion velocity across children
    private CompletionVelocity completionVelocity;

    // Health score from last week for trend calculation
    // Frontend derives trend: current vs lastWeek
    // IMPROVING if diff > 3, DECLINING if diff < -3, else STABLE
    private Double healthScoreLastWeek;

    public static class ChildrenSummary {
        private Integer total;
        private Integer thriving;
        private Integer onTrack;
        private Integer atRisk;
        private Integer critical;
        private Integer untracked;

        public ChildrenSummary() {}

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        public Integer getThriving() { return thriving; }
        public void setThriving(Integer thriving) { this.thriving = thriving; }
        public Integer getOnTrack() { return onTrack; }
        public void setOnTrack(Integer onTrack) { this.onTrack = onTrack; }
        public Integer getAtRisk() { return atRisk; }
        public void setAtRisk(Integer atRisk) { this.atRisk = atRisk; }
        public Integer getCritical() { return critical; }
        public void setCritical(Integer critical) { this.critical = critical; }
        public Integer getUntracked() { return untracked; }
        public void setUntracked(Integer untracked) { this.untracked = untracked; }
    }

    public static class WeakestChild {
        private Long id;
        private String title;
        private Double healthScore;
        private HealthStatus healthStatus;

        public WeakestChild() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Double getHealthScore() { return healthScore; }
        public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
        public HealthStatus getHealthStatus() { return healthStatus; }
        public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }
    }

    public static class CompletionVelocity {
        private Integer onTrack;
        private Integer slipping;
        private Integer completed;

        public CompletionVelocity() {}

        public Integer getOnTrack() { return onTrack; }
        public void setOnTrack(Integer onTrack) { this.onTrack = onTrack; }
        public Integer getSlipping() { return slipping; }
        public void setSlipping(Integer slipping) { this.slipping = slipping; }
        public Integer getCompleted() { return completed; }
        public void setCompleted(Integer completed) { this.completed = completed; }
    }

    public ParentInsights() {}

    public ChildrenSummary getChildrenSummary() { return childrenSummary; }
    public void setChildrenSummary(ChildrenSummary childrenSummary) { this.childrenSummary = childrenSummary; }
    public WeakestChild getWeakestChild() { return weakestChild; }
    public void setWeakestChild(WeakestChild weakestChild) { this.weakestChild = weakestChild; }
    public CompletionVelocity getCompletionVelocity() { return completionVelocity; }
    public void setCompletionVelocity(CompletionVelocity completionVelocity) { this.completionVelocity = completionVelocity; }
    public Double getHealthScoreLastWeek() { return healthScoreLastWeek; }
    public void setHealthScoreLastWeek(Double healthScoreLastWeek) { this.healthScoreLastWeek = healthScoreLastWeek; }
}
