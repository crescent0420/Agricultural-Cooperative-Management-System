
package com.acms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FarmerRegistrationRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 150, message = "fullName must be at most 150 characters")
        String fullName,

        @NotBlank(message = "nationalId is required")
        @Pattern(regexp = "\\d{16}", message = "nationalId must be exactly 16 digits")
        String nationalId,

        @Pattern(regexp = "\\+?[0-9]{9,15}",
                message = "phoneNumber must contain 9 to 15 digits, optionally starting with +")
        String phoneNumber,

        @NotNull(message = "cooperativeId is required")
        Long cooperativeId) {
}
