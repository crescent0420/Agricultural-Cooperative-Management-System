package com.acms.dto.report;

import java.math.BigDecimal;
import java.util.List;

/** All the rows of the grade-distribution report together. */
public record GradeDistributionTotals(
        long offerings,
        long published,
        long notYetPublished,
        BigDecimal averageFinalScore,
        List<GradeCount> grades) {
}
