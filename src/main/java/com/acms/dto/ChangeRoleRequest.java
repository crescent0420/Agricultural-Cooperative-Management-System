
package com.acms.dto;

import com.acms.entity.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull Role role,
        Long districtId,
        Long farmerId
) {
}
