package com.acms.exception;

/** Thrown when a business rule forbids the action (farmer not Active, below 30% paid...). Becomes HTTP 422. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}