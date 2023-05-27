package com.example.holosteganograph.exceptions;

public class FileNotUploadedException extends RuntimeException {
    public FileNotUploadedException(String message) {
        super(message);
    }
}
