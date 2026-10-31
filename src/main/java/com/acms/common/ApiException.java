package com.acms.common;

import org.springframework.http.HttpStatus;

/**
 * Parent of our own business exceptions. Each subclass knows its HTTP status, so the
 * GlobalExceptionHandler needs only ONE method for all of them.
 *   ResourceNotFoundException 404, ConflictException 409, BusinessRuleException 422, BadRequestException 400
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
