
package com.acms.dto;

import com.acms.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9._-]{3,50}")
        String username,

        @NotBlank
        @Size(min = 12, max = 128)
        String password,

        @NotNull
        Role role,

        Long districtId,

        Long farmerId
) {
}
