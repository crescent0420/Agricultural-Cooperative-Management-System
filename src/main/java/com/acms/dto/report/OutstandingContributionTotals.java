package com.acms.dto.report;

import java.math.BigDecimal;

/** The sum of all the rows of the outstanding-contributions report. */
public record OutstandingContributionTotals(
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
