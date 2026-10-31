package com.acms.common;

import java.util.List;

/** The "error" part of an ApiResponse: HTTP status, its name, the URL, and invalid fields (validation only). */
public record ApiError(
        int status,
        String error,
        String path,
        List<FieldIssue> fieldErrors) {

    /** One invalid field, used only for validation errors. */
    public record FieldIssue(String field, String message) {
    }
}
