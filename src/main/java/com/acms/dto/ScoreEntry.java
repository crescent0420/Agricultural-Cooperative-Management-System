package com.acms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** One score; the upper limit (the inspection's maxScore) is checked in the service. */
public record ScoreEntry(
        @NotNull(message = "inspectionId is required")
        Long inspectionId,

        @NotNull(message = "score is required")
        @DecimalMin(value = "0.00", message = "score cannot be negative")
        @Digits(integer = 4, fraction = 2, message = "score allows up to 4 digits and 2 decimals")
        BigDecimal score) {
}
