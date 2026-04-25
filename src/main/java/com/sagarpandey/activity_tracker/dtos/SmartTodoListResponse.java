package com.sagarpandey.activity_tracker.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SmartTodoListResponse {

    private LocalDate date;
    private String timezone;
    private String listType; // TODAY or DATE
    private Summary summary;
    private List<SmartTodoResponse> items;
    private LocalDateTime generatedAt;

    public SmartTodoListResponse() {}

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getListType() { return listType; }
    public void setListType(String listType) { this.listType = listType; }

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    public List<SmartTodoResponse> getItems() { return items; }
    public void setItems(List<SmartTodoResponse> items) { this.items = items; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public static class Summary {
        private Integer totalItems;
        private Integer mustDoTodayCount;
        private Integer catchUpTodayCount;
        private Integer goodToDoTodayCount;
        private Integer completedTodayCount;
        private List<Long> recommendedFocusGoalIds;
        private List<String> recommendedFocusTitles;

        public Summary() {}

        public Integer getTotalItems() { return totalItems; }
        public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }

        public Integer getMustDoTodayCount() { return mustDoTodayCount; }
        public void setMustDoTodayCount(Integer mustDoTodayCount) { this.mustDoTodayCount = mustDoTodayCount; }

        public Integer getCatchUpTodayCount() { return catchUpTodayCount; }
        public void setCatchUpTodayCount(Integer catchUpTodayCount) { this.catchUpTodayCount = catchUpTodayCount; }

        public Integer getGoodToDoTodayCount() { return goodToDoTodayCount; }
        public void setGoodToDoTodayCount(Integer goodToDoTodayCount) { this.goodToDoTodayCount = goodToDoTodayCount; }

        public Integer getCompletedTodayCount() { return completedTodayCount; }
        public void setCompletedTodayCount(Integer completedTodayCount) { this.completedTodayCount = completedTodayCount; }

        public List<Long> getRecommendedFocusGoalIds() { return recommendedFocusGoalIds; }
        public void setRecommendedFocusGoalIds(List<Long> recommendedFocusGoalIds) { this.recommendedFocusGoalIds = recommendedFocusGoalIds; }

        public List<String> getRecommendedFocusTitles() { return recommendedFocusTitles; }
        public void setRecommendedFocusTitles(List<String> recommendedFocusTitles) { this.recommendedFocusTitles = recommendedFocusTitles; }
    }
}
