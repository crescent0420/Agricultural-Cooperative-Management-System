package com.acms.dto;

import com.acms.entity.Role;
import com.acms.security.Permission;
import java.util.List;

/** Answer of GET /api/auth/me: who this token belongs to, from the database right now. */
public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean emailVerified,
        boolean mustChangePassword,
        Long districtId,
        Long farmerId,
        List<Permission> permissions) {   // empty while the one-time password must still be changed
}
