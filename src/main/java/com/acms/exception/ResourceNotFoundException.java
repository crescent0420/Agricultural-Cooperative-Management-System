package com.acms.exception;

/** Thrown when a record that was asked for does not exist. Becomes HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}