package com.acms.dto.report;

import java.math.BigDecimal;

/** The input contributions of one season in one district. */
public record OutstandingContributionRow(
        Long seasonId,
        String seasonName,
        Long districtId,
        String districtName,
        long contributions,
        BigDecimal amountDue,
        BigDecimal confirmedPaid,
        BigDecimal pendingPayments,
        BigDecimal outstanding,
        BigDecimal collectedPercent,
        long fullyPaid,
        long partlyPaid,
        long unpaid) {
}
