package com.acms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** PUT /api/offerings/{id}. The programme and the season cannot change: that would be another offering. */
public record ProgrammeOfferingUpdateRequest(
        @NotNull(message = "fieldOfficerId is required")
        Long fieldOfficerId,

        @NotNull(message = "hectaresPerParticipant is required")
        @DecimalMin(value = "0.01", message = "hectaresPerParticipant must be at least 0.01")
        @Digits(integer = 4, fraction = 2, message = "hectaresPerParticipant allows up to 4 digits and 2 decimals")
        BigDecimal hectaresPerParticipant,

        @NotNull(message = "maxParticipants is required")
        @Min(value = 1, message = "maxParticipants must be at least 1")
        @Max(value = 100000, message = "maxParticipants must be at most 100000")
        Integer maxParticipants) {
}
