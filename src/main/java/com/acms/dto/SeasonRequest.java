package com.acms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SeasonRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "startsOn is required")
        LocalDate startsOn,

        @NotNull(message = "endsOn is required")
        LocalDate endsOn,

        @NotNull(message = "maxLandHectares is required")
        @DecimalMin(value = "0.01", message = "maxLandHectares must be greater than 0")
        @Digits(integer = 6, fraction = 2, message = "maxLandHectares allows at most 2 decimal places")
        BigDecimal maxLandHectares,

        @NotNull(message = "distributionDate is required")
        LocalDate distributionDate) {
}
