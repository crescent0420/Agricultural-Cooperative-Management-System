
package com.acms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FarmerUpdateRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 150, message = "fullName must be at most 150 characters")
        String fullName,

        @Pattern(regexp = "\\+?[0-9]{9,15}",
                message = "phoneNumber must contain 9 to 15 digits, optionally starting with +")
        String phoneNumber) {
}
