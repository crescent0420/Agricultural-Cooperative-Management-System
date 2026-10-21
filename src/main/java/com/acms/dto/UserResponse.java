
package com.acms.dto;

import com.acms.entity.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        Role role,
        boolean enabled,
        Long districtId,
        Long farmerId,
        Instant createdAt,
        Instant updatedAt
) {
}
