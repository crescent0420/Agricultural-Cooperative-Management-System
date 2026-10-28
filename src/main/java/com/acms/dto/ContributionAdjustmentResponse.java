package com.acms.dto;

import com.acms.entity.ContributionAdjustment;
import java.math.BigDecimal;
import java.time.Instant;

public record ContributionAdjustmentResponse(
        Long id,
        BigDecimal oldAmount,
        BigDecimal newAmount,
        String reason,
        Long adjustedById,
        String adjustedByUsername,
        Instant adjustedAt) {

    public static ContributionAdjustmentResponse from(ContributionAdjustment adjustment) {
        return new ContributionAdjustmentResponse(
                adjustment.getId(),
                adjustment.getOldAmount(),
                adjustment.getNewAmount(),
                adjustment.getReason(),
                adjustment.getAdjustedBy().getId(),
                adjustment.getAdjustedBy().getUsername(),
                adjustment.getAdjustedAt());
    }
}
