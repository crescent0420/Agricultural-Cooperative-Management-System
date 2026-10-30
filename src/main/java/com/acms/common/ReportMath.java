package com.acms.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** The rounding rules of the management reports, in one place. Always BigDecimal, never double. */
public final class ReportMath {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ReportMath() {
    }

    /** part x 100 / whole, rounded half-up to 2 decimals (capacity used, share of a grade). null when whole is 0. */
    public static BigDecimal percent(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    /**
     * Money collected: paid x 100 / due, rounded DOWN to 2 decimals. This is the same rule as a farmer's own
     * balance (ContributionBalance.paidPercent; ReportMathTest checks that they agree), so a report never shows
     * more collected than the balances do. null when nothing is due.
     */
    public static BigDecimal collectedPercent(BigDecimal paid, BigDecimal due) {
        if (due == null || due.signum() == 0) {
            return null;
        }
        return paid.multiply(HUNDRED).divide(due, 2, RoundingMode.DOWN);
    }

    /** sum / count, rounded half-up to 2 decimals (average final score). null when count is 0. */
    public static BigDecimal average(BigDecimal sum, long count) {
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    /** Money with exactly 2 decimals, so 1000 and 1000.00 are written the same way. */
    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
