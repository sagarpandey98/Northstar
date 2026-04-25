package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.dtos.SmartTodoListResponse;

/**
 * Smart Todo Service Interface
 * Provides intelligent daily task generation based on schedule awareness,
 * priority ordering, urgency calculation, and progress tracking.
 */
public interface SmartTodoService {
    
    /**
     * Get today's smart todo list
     * Returns intelligent daily tasks based on schedule, priority, and progress
     */
    SmartTodoListResponse getTodaySmartTodos(String userId);
    
    /**
     * Refresh today's todo list
     * Recalculates priorities based on new activities, streak changes
     * 
     * @param userId user ID
     * @return refreshed todo list
     */
    SmartTodoListResponse refreshTodayTodos(String userId);
    
    /**
     * Get smart todo list for a specific date
     * Returns intelligent daily tasks based on schedule, priority, and progress
     * 
     * @param userId user ID
     * @param date target date for todo calculation
     * @return todo list for specified date
     */
    SmartTodoListResponse getSmartTodosForDate(String userId, java.time.LocalDate date);
}
