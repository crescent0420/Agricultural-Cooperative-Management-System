package com.acms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Pattern(regexp = "[a-z0-9._-]{3,50}",
                message = "username must be 3 to 50 characters: lowercase letters, digits, dot, underscore or hyphen")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 254, message = "email is too long")
        String email) {
}
