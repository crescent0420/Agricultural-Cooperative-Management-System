package com.acms.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** POST /api/offerings/{id}/inspections and PUT /api/inspections/{id} */
public record InspectionRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "maxScore is required")
        @DecimalMin(value = "0.01", message = "maxScore must be at least 0.01")
        @Digits(integer = 4, fraction = 2, message = "maxScore allows up to 4 digits and 2 decimals")
        BigDecimal maxScore,

        @NotNull(message = "weight is required")
        @DecimalMin(value = "0.01", message = "weight must be at least 0.01 (percent)")
        @DecimalMax(value = "100.00", message = "weight must be at most 100 (percent)")
        @Digits(integer = 3, fraction = 2, message = "weight allows up to 2 decimals")
        BigDecimal weight) {
}
