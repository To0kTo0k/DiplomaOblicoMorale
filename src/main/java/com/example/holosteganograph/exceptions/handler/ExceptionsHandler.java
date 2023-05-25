package com.example.holosteganograph.exceptions.handler;

import com.example.holosteganograph.exceptions.response.ExceptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ExceptionsHandler {

    static final Logger log = LoggerFactory.getLogger(ExceptionsHandler.class);

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public ExceptionResponse handleException(Exception e) {
        log.error(e.getMessage(), e);
        return new ExceptionResponse(String.format("%s %s", LocalDateTime.now(), e.getMessage()));
    }
}
