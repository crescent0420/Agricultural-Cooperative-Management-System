package com.acms.exception;

import com.acms.common.ApiException;
import org.springframework.http.HttpStatus;

/** HTTP 400, a request value is not acceptable. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
