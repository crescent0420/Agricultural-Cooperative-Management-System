package com.acms.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * The one envelope EVERY endpoint answers with, success or error:
 *
 *   { "success": true,  "message": "OK", "data": { ...the DTO... }, "timestamp": "..." }
 *   { "success": false, "message": "Validation failed", "error": { status, error, path, fieldErrors }, "timestamp": "..." }
 *
 * "data" is always a DTO (or a PagedResponse of DTOs), never an entity: entities stay inside the services.
 * Null parts ("data" on an error, "error" on a success) are left out of the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("OK", data);
    }

    /** A success that carries only a message (for example "Email address verified."). */
    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null, null, Instant.now());
    }

    public static ApiResponse<Void> failure(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error, Instant.now());
    }
}
