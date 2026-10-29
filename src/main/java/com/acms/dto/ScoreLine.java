package com.acms.dto;

import java.math.BigDecimal;

/** originalScore = as entered by the Field Officer; currentScore = after any corrections. */
public record ScoreLine(
        Long inspectionId,
        String inspectionName,
        BigDecimal maxScore,
        BigDecimal weight,
        BigDecimal originalScore,
        BigDecimal currentScore,
        boolean corrected) {
}
