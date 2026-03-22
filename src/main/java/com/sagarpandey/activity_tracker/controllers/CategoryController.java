package com.sagarpandey.activity_tracker.controllers;

import com.sagarpandey.activity_tracker.Service.V1.CategoryManagementService;
import com.sagarpandey.activity_tracker.dtos.CategoryRequest;
import com.sagarpandey.activity_tracker.dtos.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CategoryController {
    
    private final CategoryManagementService categoryManagementService;

    public CategoryController(CategoryManagementService categoryManagementService) {
        this.categoryManagementService = categoryManagementService;
    }

    /**
     * Get the complete category structure for a user
     * Example: GET /api/v1/categories/user123
     */
    @GetMapping
    public ResponseEntity<CategoryResponse> getCategoryStructure(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaim("id"); // Extract user ID from the token
        System.out.println(userId);
        CategoryResponse response = categoryManagementService.getCategoryStructure(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the complete category structure for a user
     * Frontend sends the entire nested structure and backend detects changes
     * Example: PUT /api/v1/categories
     */
    @PutMapping
    public ResponseEntity<CategoryResponse> updateCategoryStructure(@Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaim("id"); // Extract user ID from the token
        CategoryResponse response = categoryManagementService.updateCategoryStructure(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Partial update of category structure for a user
     * Same functionality as PUT but semantically represents partial updates
     * Example: PATCH /api/v1/categories
     */
    @PatchMapping
    public ResponseEntity<CategoryResponse> patchCategoryStructure(@Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaim("id"); // Extract user ID from the token
        CategoryResponse response = categoryManagementService.patchCategoryStructure(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Alternative endpoint for creating/updating via POST
     * Same functionality as PUT but follows REST convention for create-or-update
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createOrUpdateCategoryStructure(@Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaim("id"); // Extract user ID from the token
        CategoryResponse response = categoryManagementService.updateCategoryStructure(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
