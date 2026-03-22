package com.sagarpandey.activity_tracker.dtos;

import lombok.Data;
import java.util.List;

@Data
public class ActivitySearchResponse {
    private List<ActivityResponse> activities;
    private PaginationInfo pagination;

    @Data
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
