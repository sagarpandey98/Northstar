package com.sagarpandey.activity_tracker.Exceptions;

public class GoalNotFoundException extends RuntimeException {
    
    public GoalNotFoundException(String message) {
        super(message);
    }
    
    public GoalNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GoalNotFoundException(Long goalId) {
        super("Goal not found with ID: " + goalId);
    }
    
    public GoalNotFoundException(String uuid, String userId) {
        super("Goal not found with UUID: " + uuid + " for user: " + userId);
    }
}
