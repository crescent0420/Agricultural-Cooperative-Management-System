package com.acms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** POST /api/contributions/{id}/adjustments: the only way an amount owed can change. */
public record ContributionAdjustmentRequest(
        @NotNull(message = "newAmountDue is required")
        @DecimalMin(value = "0.01", message = "newAmountDue must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "newAmountDue allows up to 10 digits and 2 decimals")
        BigDecimal newAmountDue,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason) {
}
