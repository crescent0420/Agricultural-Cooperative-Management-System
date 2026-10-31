package com.acms.common;

import org.springframework.security.core.Authentication;

/** Parent of every controller: wraps results in the ApiResponse envelope. */
public abstract class BaseController {

    protected static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data);
    }

    protected static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.success(message, data);
    }

    protected static <T> ApiResponse<T> created(T data) {
        return ApiResponse.success("Created", data);
    }

    protected static ApiResponse<Void> message(MessageResponse response) {
        return ApiResponse.message(response.message());
    }

    /** The logged-in user's id: the token's subject, set by DatabaseBackedJwtConverter. */
    protected static Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
