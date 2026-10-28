package com.acms.exception;

import com.acms.common.ApiException;
import org.springframework.http.HttpStatus;

/** HTTP 409, the request clashes with existing data (duplicate, already enrolled...). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
