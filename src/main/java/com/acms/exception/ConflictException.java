package com.acms.exception;

/** Thrown when the request clashes with existing data (duplicate, already enrolled...). Becomes HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}