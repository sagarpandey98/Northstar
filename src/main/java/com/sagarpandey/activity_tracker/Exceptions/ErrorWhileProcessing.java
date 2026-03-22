package com.sagarpandey.activity_tracker.Exceptions;

public class ErrorWhileProcessing extends RuntimeException {
    public ErrorWhileProcessing(String message, Throwable cause) {
        super(message, cause);
    }
}