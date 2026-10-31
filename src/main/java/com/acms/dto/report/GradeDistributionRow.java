package com.acms.dto.report;

import java.math.BigDecimal;
import java.util.List;

/** The published grades of one programme offering. totalFinalScore = the sum of their final scores. */
public record GradeDistributionRow(
        Long offeringId,
        String programmeName,
        Long seasonId,
        String seasonName,
        Long districtId,
        String districtName,
        String fieldOfficerUsername,
        long activeEnrolments,
        long published,
        long notYetPublished,
        BigDecimal totalFinalScore,
        BigDecimal averageFinalScore,
        List<GradeCount> grades) {
}
