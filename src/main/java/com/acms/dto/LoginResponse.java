package com.acms.dto;

/** mustChangePassword = true means: the only thing this token can do is change the password. */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword) {
}
