package com.acms.exception;

import com.acms.common.ApiException;
import org.springframework.http.HttpStatus;

/** HTTP 404, the requested record does not exist. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
