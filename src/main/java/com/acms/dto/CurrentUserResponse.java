package com.acms.dto;

import com.acms.entity.Role;

/** Answer of GET /api/auth/me: who this token belongs to, from the database right now. */
public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean emailVerified,
        boolean mustChangePassword,
        Long districtId,
        Long farmerId) {
}
