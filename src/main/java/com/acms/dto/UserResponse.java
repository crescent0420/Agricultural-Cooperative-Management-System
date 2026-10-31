package com.acms.dto;

import com.acms.enums.Role;
import java.time.Instant;

/** Deliberately has no password, hash or token-version field. */
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        boolean emailVerified,
        boolean mustChangePassword,
        Long districtId,
        Long farmerId,
        Instant createdAt,
        Instant updatedAt) {
}
