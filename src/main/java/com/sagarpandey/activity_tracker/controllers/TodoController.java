package com.sagarpandey.activity_tracker.controllers;

import com.sagarpandey.activity_tracker.Service.Interface.SmartTodoService;
import com.sagarpandey.activity_tracker.dtos.SmartTodoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Todo Controller for Smart Todo List functionality
 * Provides intelligent daily task management instead of direct priority exposure
 */
@RestController
@RequestMapping("/api/v1/todos")
@CrossOrigin(origins = "*")
public class TodoController {
    
    private final SmartTodoService smartTodoService;
    
    @Autowired
    public TodoController(SmartTodoService smartTodoService) {
        this.smartTodoService = smartTodoService;
    }
    
    /**
     * Extract userId from JWT token
     */
    private String extractUserIdFromJwt(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("id");
        }
        throw new RuntimeException("User not authenticated");
    }
    
    /**
     * Get today's smart todo list
     * Returns intelligent daily tasks based on schedule, priority, and progress
     */
    @GetMapping("/today")
    public ResponseEntity<List<SmartTodoResponse>> getTodaySmartTodos(
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        List<SmartTodoResponse> smartTodos = smartTodoService.getTodaySmartTodos(userId);
        return ResponseEntity.ok(smartTodos);
    }
    
    
    /**
     * Refresh today's todo list
     * Recalculates priorities based on new activities
     */
    @PostMapping("/refresh")
    public ResponseEntity<List<SmartTodoResponse>> refreshTodayTodos(
            Authentication authentication) {
        
        String userId = extractUserIdFromJwt(authentication);
        List<SmartTodoResponse> refreshedTodos = smartTodoService.refreshTodayTodos(userId);
        return ResponseEntity.ok(refreshedTodos);
    }
}
