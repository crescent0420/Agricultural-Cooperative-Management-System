package com.acms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** POST /api/contributions: what one farmer owes for one season's inputs. */
public record ContributionRequest(
        @NotNull(message = "farmerId is required")
        Long farmerId,

        @NotNull(message = "seasonId is required")
        Long seasonId,

        @NotNull(message = "amountDue is required")
        @DecimalMin(value = "0.01", message = "amountDue must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "amountDue allows up to 10 digits and 2 decimals")
        BigDecimal amountDue) {
}
