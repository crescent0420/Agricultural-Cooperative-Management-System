package com.acms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A contribution's balance at one moment.
 *   outstanding = amountDue - confirmedPaid      (pending payments do NOT count until confirmed)
 * Every comparison uses BigDecimal, never double, so 30% of 10 000.00 is exactly 3 000.00.
 */
public record ContributionBalance(BigDecimal amountDue, BigDecimal confirmedPaid, BigDecimal pending) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static ContributionBalance of(BigDecimal amountDue, BigDecimal confirmedPaid, BigDecimal pending) {
        return new ContributionBalance(amountDue, zeroIfNull(confirmedPaid), zeroIfNull(pending));
    }

    public BigDecimal outstanding() {
        return amountDue.subtract(confirmedPaid);
    }

    /** What may still be requested: what is owed minus the payments already waiting for confirmation. */
    public BigDecimal requestable() {
        return outstanding().subtract(pending);
    }

    /** Rounded DOWN (29.999% shows as 29.99%), so the number shown never suggests more than was paid. */
    public BigDecimal paidPercent() {
        return confirmedPaid.multiply(HUNDRED).divide(amountDue, 2, RoundingMode.DOWN);
    }

    /** confirmedPaid >= percent% of amountDue, compared exactly: confirmedPaid * 100 >= amountDue * percent. */
    public boolean meets(int percent) {
        return confirmedPaid.multiply(HUNDRED).compareTo(amountDue.multiply(BigDecimal.valueOf(percent))) >= 0;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
