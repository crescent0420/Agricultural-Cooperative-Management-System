
package com.acms.exception;

/**
 * Thrown when a request parameter is not acceptable.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
