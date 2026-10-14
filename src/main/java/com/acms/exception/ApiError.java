package com.acms.exception;

import java.util.List;

/** The one JSON shape used for every error response. */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldIssue> fieldErrors) {

    /** One invalid field, used only for validation errors. */
    public record FieldIssue(String field, String message) {
    }
}