package com.example.holosteganograph.exceptions.handler;

import com.example.holosteganograph.exceptions.response.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class ExceptionsHandler {
    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleException(Exception e) {
        String message =String.format("%s%s", LocalDateTime.now(), e.getMessage());
        ExceptionResponse response = new ExceptionResponse(message);
        return new ResponseEntity<>(response, HttpStatus.NOT_MODIFIED);
    }
}
