package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.dtos.ActivitySearchRequest;
import com.sagarpandey.activity_tracker.models.Activity;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class ActivitySpecifications {

    public static Specification<Activity> buildSpecification(String userId, ActivitySearchRequest.FilterCriteria filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Always filter by userId
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            
            if (filter != null) {
                // Time range filters
                if (filter.getStartTime() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), filter.getStartTime()));
                }
                if (filter.getEndTime() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), filter.getEndTime()));
                }
                
                // Activity field filters
                if (filter.getName() != null && !filter.getName().trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")), 
                        "%" + filter.getName().toLowerCase() + "%"
                    ));
                }
                
                if (filter.getDescription() != null && !filter.getDescription().trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), 
                        "%" + filter.getDescription().toLowerCase() + "%"
                    ));
                }
                
                // Domain/Subdomain/Specific filters using direct fields
                if (filter.getDomainId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("domainId"), filter.getDomainId()));
                }
                if (filter.getDomainName() != null && !filter.getDomainName().trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("domainName")), 
                        "%" + filter.getDomainName().toLowerCase() + "%"
                    ));
                }
                
                if (filter.getSubdomainId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("subdomainId"), filter.getSubdomainId()));
                }
                if (filter.getSubdomainName() != null && !filter.getSubdomainName().trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("subdomainName")), 
                        "%" + filter.getSubdomainName().toLowerCase() + "%"
                    ));
                }
                
                if (filter.getSpecificId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("specificId"), filter.getSpecificId()));
                }
                if (filter.getSpecificName() != null && !filter.getSpecificName().trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("specificName")), 
                        "%" + filter.getSpecificName().toLowerCase() + "%"
                    ));
                }
                
                // Legacy specificActivityId filter (deprecated - no longer used)
                if (filter.getSpecificActivityId() != null && !filter.getSpecificActivityId().trim().isEmpty()) {
                    // This field no longer exists in the entity, so we ignore it
                    // Could log a deprecation warning here
                }
                
                // Mood range filters
                if (filter.getMinMood() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("mood"), filter.getMinMood()));
                }
                if (filter.getMaxMood() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("mood"), filter.getMaxMood()));
                }
                
                // Rating range filters
                if (filter.getMinRating() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), filter.getMinRating()));
                }
                if (filter.getMaxRating() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("rating"), filter.getMaxRating()));
                }
                
                // General search term across multiple fields
                if (filter.getSearchTerm() != null && !filter.getSearchTerm().trim().isEmpty()) {
                    String searchPattern = "%" + filter.getSearchTerm().toLowerCase() + "%";
                    Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern),
                        // Search in domain/subdomain/specific names
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("domainName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("subdomainName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("specificName")), searchPattern)
                    );
                    predicates.add(searchPredicate);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
