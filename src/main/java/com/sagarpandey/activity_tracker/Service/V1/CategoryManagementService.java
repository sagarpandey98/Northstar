package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.DomainRepository;
import com.sagarpandey.activity_tracker.Repository.SubdomainRepository;
import com.sagarpandey.activity_tracker.Repository.SpecificRepository;
import com.sagarpandey.activity_tracker.dtos.CategoryRequest;
import com.sagarpandey.activity_tracker.dtos.CategoryResponse;
import com.sagarpandey.activity_tracker.models.Domain;
import com.sagarpandey.activity_tracker.models.Subdomain;
import com.sagarpandey.activity_tracker.models.Specifics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryManagementService {

    private final DomainRepository domainRepository;
    private final SubdomainRepository subdomainRepository;
    private final SpecificRepository specificRepository;

    public CategoryManagementService(DomainRepository domainRepository,
                                   SubdomainRepository subdomainRepository,
                                   SpecificRepository specificRepository) {
        this.domainRepository = domainRepository;
        this.subdomainRepository = subdomainRepository;
        this.specificRepository = specificRepository;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryStructure(String userId) {
        System.out.println("userId" + userId);
        List<Domain> domains = domainRepository.findAllByUserId(userId);
        System.out.println("domains" + domains);
        CategoryResponse response = new CategoryResponse();
        response.setName("User Categories");
        response.setUuid(userId);
        response.setDomains(convertDomainsToDto(domains));
        return response;
    }

    @Transactional
    public CategoryResponse updateCategoryStructure(CategoryRequest request, String userId) {
        // Get existing data from database
        List<Domain> existingDomains = domainRepository.findAllByUserId(userId);
        Map<UUID, Domain> existingDomainMap = existingDomains.stream()
                .collect(Collectors.toMap(Domain::getUuid, d -> d));
        
        List<Subdomain> existingSubdomains = subdomainRepository.findAllByUserId(userId);
        Map<UUID, Subdomain> existingSubdomainMap = existingSubdomains.stream()
                .collect(Collectors.toMap(Subdomain::getUuid, s -> s));
        
        List<Specifics> existingSpecifics = specificRepository.findAllByUserId(userId);
        Map<UUID, Specifics> existingSpecificMap = existingSpecifics.stream()
                .collect(Collectors.toMap(Specifics::getUuid, s -> s));

        // Process domains
        Set<UUID> processedDomainUuids = new HashSet<>();
        
        if (request.getDomains() != null) {
            for (CategoryRequest.DomainDto domainDto : request.getDomains()) {
                Domain domain = processDomain(domainDto, userId, existingDomainMap);
                processedDomainUuids.add(domain.getUuid());
                
                // Process subdomains
                Set<UUID> processedSubdomainUuids = new HashSet<>();
                if (domainDto.getSubDomains() != null) {
                    for (CategoryRequest.SubDomainDto subdomainDto : domainDto.getSubDomains()) {
                        Subdomain subdomain = processSubdomain(subdomainDto, domain, userId, existingSubdomainMap);
                        processedSubdomainUuids.add(subdomain.getUuid());
                        
                        // Process specifics
                        Set<UUID> processedSpecificUuids = new HashSet<>();
                        if (subdomainDto.getSpecifics() != null) {
                            for (CategoryRequest.SpecificDto specificDto : subdomainDto.getSpecifics()) {
                                Specifics specific = processSpecific(specificDto, subdomain, userId, existingSpecificMap);
                                processedSpecificUuids.add(specific.getUuid());
                            }
                        }
                        
                        // Delete specifics that weren't in the request
                        deleteUnprocessedSpecifics(subdomain, processedSpecificUuids, existingSpecificMap);
                    }
                }
                
                // Delete subdomains that weren't in the request
                deleteUnprocessedSubdomains(domain, processedSubdomainUuids, existingSubdomainMap);
            }
        }
        
        // Delete domains that weren't in the request
        deleteUnprocessedDomains(userId, processedDomainUuids, existingDomainMap);

        // Return updated complete structure - ensures frontend gets consistent data
        return getCategoryStructure(userId);
    }

    /**
     * Alias method for patch operations - same functionality as update
     * Both PUT and PATCH return complete structure for frontend consistency
     */
    @Transactional
    public CategoryResponse patchCategoryStructure(CategoryRequest request, String userId) {
        // Use the same logic as update - return complete structure
        return updateCategoryStructure(request, userId);
    }

    private Domain processDomain(CategoryRequest.DomainDto domainDto, String userId, Map<UUID, Domain> existingDomainMap) {
        Domain domain;
        
        if (domainDto.getUuid() == null || domainDto.getUuid().isEmpty()) {
            // CREATE - New domain
            domain = new Domain();
            domain.setUuid(UUID.randomUUID());
            domain.setUserId(userId);
            domain.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        } else {
            // UPDATE - Existing domain
            UUID uuid = UUID.fromString(domainDto.getUuid());
            domain = existingDomainMap.get(uuid);
            if (domain == null) {
                throw new IllegalArgumentException("Domain with UUID " + uuid + " not found");
            }
        }
        
        domain.setName(domainDto.getName());
        domain.setDescription(domainDto.getDescription());
        domain.setLastUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        
        return domainRepository.save(domain);
    }

    private Subdomain processSubdomain(CategoryRequest.SubDomainDto subdomainDto, Domain domain, String userId, Map<UUID, Subdomain> existingSubdomainMap) {
        Subdomain subdomain;
        
        if (subdomainDto.getUuid() == null || subdomainDto.getUuid().isEmpty()) {
            // CREATE - New subdomain
            subdomain = new Subdomain();
            subdomain.setUuid(UUID.randomUUID());
            subdomain.setUserId(userId);
            subdomain.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        } else {
            // UPDATE - Existing subdomain
            UUID uuid = UUID.fromString(subdomainDto.getUuid());
            subdomain = existingSubdomainMap.get(uuid);
            if (subdomain == null) {
                throw new IllegalArgumentException("Subdomain with UUID " + uuid + " not found");
            }
        }
        
        subdomain.setName(subdomainDto.getName());
        subdomain.setDescription(subdomainDto.getDescription());
        subdomain.setDomain(domain);
        subdomain.setLastUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        
        return subdomainRepository.save(subdomain);
    }

    private Specifics processSpecific(CategoryRequest.SpecificDto specificDto, Subdomain subdomain, String userId, Map<UUID, Specifics> existingSpecificMap) {
        Specifics specific;
        
        if (specificDto.getUuid() == null || specificDto.getUuid().isEmpty()) {
            // CREATE - New specific
            specific = new Specifics();
            specific.setUuid(UUID.randomUUID());
            specific.setUserId(userId);
            specific.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        } else {
            // UPDATE - Existing specific
            UUID uuid = UUID.fromString(specificDto.getUuid());
            specific = existingSpecificMap.get(uuid);
            if (specific == null) {
                throw new IllegalArgumentException("Specific with UUID " + uuid + " not found");
            }
        }
        
        specific.setName(specificDto.getName());
        specific.setDescription(specificDto.getDescription());
        specific.setSubdomain(subdomain);
        specific.setLastUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        
        return specificRepository.save(specific);
    }

    private void deleteUnprocessedDomains(String userId, Set<UUID> processedUuids, Map<UUID, Domain> existingDomainMap) {
        List<Domain> toDelete = existingDomainMap.values().stream()
                .filter(domain -> !processedUuids.contains(domain.getUuid()))
                .collect(Collectors.toList());
        
        if (!toDelete.isEmpty()) {
            domainRepository.deleteAll(toDelete);
        }
    }

    private void deleteUnprocessedSubdomains(Domain domain, Set<UUID> processedUuids, Map<UUID, Subdomain> existingSubdomainMap) {
        List<Subdomain> toDelete = existingSubdomainMap.values().stream()
                .filter(subdomain -> subdomain.getDomain().getId().equals(domain.getId()) && 
                                   !processedUuids.contains(subdomain.getUuid()))
                .collect(Collectors.toList());
        
        if (!toDelete.isEmpty()) {
            subdomainRepository.deleteAll(toDelete);
        }
    }

    private void deleteUnprocessedSpecifics(Subdomain subdomain, Set<UUID> processedUuids, Map<UUID, Specifics> existingSpecificMap) {
        List<Specifics> toDelete = existingSpecificMap.values().stream()
                .filter(specific -> specific.getSubdomain().getId().equals(subdomain.getId()) && 
                                  !processedUuids.contains(specific.getUuid()))
                .collect(Collectors.toList());
        
        if (!toDelete.isEmpty()) {
            specificRepository.deleteAll(toDelete);
        }
    }

    private List<CategoryResponse.DomainDto> convertDomainsToDto(List<Domain> domains) {
        return domains.stream().map(this::convertDomainToDto).collect(Collectors.toList());
    }

    private CategoryResponse.DomainDto convertDomainToDto(Domain domain) {
        CategoryResponse.DomainDto dto = new CategoryResponse.DomainDto();
        dto.setName(domain.getName());
        dto.setUuid(domain.getUuid().toString());
        dto.setDescription(domain.getDescription());
        
        if (domain.getSubdomains() != null) {
            dto.setSubDomains(domain.getSubdomains().stream()
                    .map(this::convertSubdomainToDto)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private CategoryResponse.SubDomainDto convertSubdomainToDto(Subdomain subdomain) {
        CategoryResponse.SubDomainDto dto = new CategoryResponse.SubDomainDto();
        dto.setName(subdomain.getName());
        dto.setUuid(subdomain.getUuid().toString());
        dto.setDescription(subdomain.getDescription());
        
        if (subdomain.getSpecifics() != null) {
            dto.setSpecifics(subdomain.getSpecifics().stream()
                    .map(this::convertSpecificToDto)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private CategoryResponse.SpecificDto convertSpecificToDto(Specifics specific) {
        CategoryResponse.SpecificDto dto = new CategoryResponse.SpecificDto();
        dto.setName(specific.getName());
        dto.setUuid(specific.getUuid().toString());
        dto.setDescription(specific.getDescription());
        return dto;
    }
}
