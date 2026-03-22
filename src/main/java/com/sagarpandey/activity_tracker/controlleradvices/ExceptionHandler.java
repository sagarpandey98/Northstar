package com.sagarpandey.activity_tracker.controlleradvices;

import com.sagarpandey.activity_tracker.Exceptions.ErrorWhileProcessing;
import com.sagarpandey.activity_tracker.Exceptions.GoalNotFoundException;
import com.sagarpandey.activity_tracker.Exceptions.ValidationException;
import com.sagarpandey.activity_tracker.dtos.ValidationErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Objects;

@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorMessage> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        ValidationErrorMessage validationErrorMessage = new ValidationErrorMessage();
        validationErrorMessage.setStatus("error");
        validationErrorMessage.setMessage(errorMessage);
        validationErrorMessage.setData(null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrorMessage);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationErrorMessage> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ValidationErrorMessage validationErrorMessage = new ValidationErrorMessage();
        validationErrorMessage.setStatus("error");
        validationErrorMessage.setMessage("Malformed JSON request");
        validationErrorMessage.setData(null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrorMessage);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ErrorWhileProcessing.class)
    public ResponseEntity<ValidationErrorMessage> handleErrorWhileProcessingException(ErrorWhileProcessing ex) {
        ValidationErrorMessage validationErrorMessage = new ValidationErrorMessage();
        validationErrorMessage.setStatus("error");
        validationErrorMessage.setMessage(ex.getMessage());
        validationErrorMessage.setData(null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(validationErrorMessage);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(GoalNotFoundException.class)
    public ResponseEntity<ValidationErrorMessage> handleGoalNotFoundException(GoalNotFoundException ex) {
        ValidationErrorMessage validationErrorMessage = new ValidationErrorMessage();
        validationErrorMessage.setStatus("error");
        validationErrorMessage.setMessage(ex.getMessage());
        validationErrorMessage.setData(null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(validationErrorMessage);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorMessage> handleValidationException(ValidationException ex) {
        ValidationErrorMessage validationErrorMessage = new ValidationErrorMessage();
        validationErrorMessage.setStatus("error");
        validationErrorMessage.setMessage(ex.getMessage());
        validationErrorMessage.setData(null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrorMessage);
    }
}