package com.sagarpandey.activity_tracker.dtos;

import lombok.Data;
import java.util.List;

@Data
public class CategoryResponse {
    private String name;
    private String uuid;
    private List<DomainDto> domains;

    @Data
    public static class DomainDto {
        private String name;
        private String uuid;
        private String description;
        private List<SubDomainDto> subDomains;
    }

    @Data
    public static class SubDomainDto {
        private String name;
        private String uuid;
        private String description;
        private List<SpecificDto> specifics;
    }

    @Data
    public static class SpecificDto {
        private String name;
        private String uuid;
        private String description;
    }
}
