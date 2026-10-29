package com.acms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** POST /api/results/{id}/corrections: the only way to change a published result. */
public record CorrectionRequest(
        @NotNull(message = "inspectionId is required")
        Long inspectionId,

        @NotNull(message = "correctedScore is required")
        @DecimalMin(value = "0.00", message = "correctedScore cannot be negative")
        @Digits(integer = 4, fraction = 2, message = "correctedScore allows up to 4 digits and 2 decimals")
        BigDecimal correctedScore,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason) {
}
