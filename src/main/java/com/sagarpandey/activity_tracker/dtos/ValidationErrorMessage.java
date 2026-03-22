package com.sagarpandey.activity_tracker.dtos;

import lombok.Data;

@Data
public class ValidationErrorMessage {
    private String status;
    private String message;
    private Object data;
}