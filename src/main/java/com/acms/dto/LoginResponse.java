package com.acms.dto;
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}