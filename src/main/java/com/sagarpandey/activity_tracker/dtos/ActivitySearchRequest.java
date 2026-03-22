package com.sagarpandey.activity_tracker.dtos;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ActivitySearchRequest {
    private FilterCriteria filter;
    private PaginationCriteria pagination;

    @Data
    public static class FilterCriteria {
        // Time range filters
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        
        // Activity field filters
        private String name;
        private String primaryCategory;
        private String secondaryCategory;
        private String tertiaryCategory;
        private String description;
        private Integer minMood;
        private Integer maxMood;
        private Integer minRating;
        private Integer maxRating;
        
        // Domain/Subdomain/Specific filters
        private String domainId;
        private String domainName;
        private String subdomainId;
        private String subdomainName;
        private String specificId;
        private String specificName;
        
        // Legacy field for backward compatibility (will be removed)
        @Deprecated
        private String specificActivityId;
        
        // Search term for general search across multiple fields
        private String searchTerm;
    }

    @Data
    public static class PaginationCriteria {
        private int page = 0; // Default to first page
        private int size = 10; // Default page size
        private String sortBy = "createdAt"; // Default sort field
        private String sortDirection = "DESC"; // Default sort direction (newest first)
    }
}
