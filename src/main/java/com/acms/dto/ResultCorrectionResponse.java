package com.acms.dto;

import com.acms.entity.ResultCorrection;
import java.math.BigDecimal;
import java.time.Instant;

public record ResultCorrectionResponse(
        Long id,
        Long inspectionId,
        String inspectionName,
        BigDecimal originalScore,
        BigDecimal correctedScore,
        BigDecimal originalFinalScore,
        BigDecimal correctedFinalScore,
        String originalGrade,
        String correctedGrade,
        String reason,
        Long correctedById,
        String correctedByUsername,
        Instant correctedAt) {

    public static ResultCorrectionResponse from(ResultCorrection c) {
        return new ResultCorrectionResponse(
                c.getId(),
                c.getInspection().getId(),
                c.getInspection().getName(),
                c.getOriginalScore(),
                c.getCorrectedScore(),
                c.getOriginalFinalScore(),
                c.getCorrectedFinalScore(),
                c.getOriginalGrade(),
                c.getCorrectedGrade(),
                c.getReason(),
                c.getCorrectedBy().getId(),
                c.getCorrectedBy().getUsername(),
                c.getCorrectedAt());
    }
}
