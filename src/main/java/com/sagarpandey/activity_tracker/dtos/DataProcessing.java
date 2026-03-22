package com.sagarpandey.activity_tracker.dtos;

public class DataProcessing extends ResponseWrapper{
    private String message;
    private String status;
    private Object data;

    public DataProcessing(String message, String status, Object data) {
        super(message, status, data);
        this.message = message;
        this.status = status;
        this.data = data;
    }
}
