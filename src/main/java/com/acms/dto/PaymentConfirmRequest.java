package com.acms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The confirmation received from the mobile money operator or the bank. */
public record PaymentConfirmRequest(
        @NotBlank(message = "providerReference is required")
        @Size(max = 100, message = "providerReference must be at most 100 characters")
        String providerReference) {
}
