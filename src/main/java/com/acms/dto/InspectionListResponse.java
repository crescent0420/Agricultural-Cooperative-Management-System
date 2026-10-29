package com.acms.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * weightsComplete: the weights add up to exactly 100, so results can be submitted.
 * frozen: a result of this offering has been submitted, so the inspections can no longer change.
 */
public record InspectionListResponse(
        Long offeringId,
        List<InspectionResponse> inspections,
        BigDecimal totalWeight,
        boolean weightsComplete,
        boolean frozen) {
}
