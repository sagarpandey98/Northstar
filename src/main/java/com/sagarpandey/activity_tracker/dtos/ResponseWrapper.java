package com.sagarpandey.activity_tracker.dtos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;


@Getter
@Setter
public class ResponseWrapper {
    private String message;
    private String status;
    private Object data;

    public ResponseWrapper(String message, String status, Object data) {
        this.message = message;
        this.status = status;
        this.data = data;
    }
}
