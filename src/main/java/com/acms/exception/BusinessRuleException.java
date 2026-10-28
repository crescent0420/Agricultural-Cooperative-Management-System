package com.acms.exception;

import com.acms.common.ApiException;
import org.springframework.http.HttpStatus;

/** HTTP 422, a business rule forbids the action (farmer not Active, below 30% paid...). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
