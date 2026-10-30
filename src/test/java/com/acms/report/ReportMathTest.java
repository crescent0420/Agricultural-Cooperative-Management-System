package com.acms.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.acms.common.ReportMath;
import com.acms.service.ContributionBalance;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests (no Spring, no database): the rounding rules of the reports. */
class ReportMathTest {

    @Test
    @DisplayName("Percentages are rounded half-up to 2 decimals; nothing to divide by gives null")
    void percent() {
        assertThat(ReportMath.percent(2, 3)).isEqualByComparingTo("66.67");
        assertThat(ReportMath.percent(1, 3)).isEqualByComparingTo("33.33");
        assertThat(ReportMath.percent(3, 3)).isEqualByComparingTo("100.00");
        assertThat(ReportMath.percent(0, 0)).isNull();
    }

    @Test
    @DisplayName("Money collected is rounded DOWN, exactly like a farmer's own balance")
    void collectedPercentMatchesTheFarmersBalance() {
        String[][] cases = {{"1400", "3000"}, {"1", "3"}, {"2999.99", "3000"}, {"1000", "1000"}, {"0", "250"}};
        for (String[] c : cases) {
            BigDecimal paid = new BigDecimal(c[0]);
            BigDecimal due = new BigDecimal(c[1]);
            assertThat(ReportMath.collectedPercent(paid, due))
                    .as(c[0] + " of " + c[1])
                    .isEqualByComparingTo(ContributionBalance.of(due, paid, BigDecimal.ZERO).paidPercent());
        }
        assertThat(ReportMath.collectedPercent(new BigDecimal("1400"), new BigDecimal("3000"))).isEqualByComparingTo("46.66");
        assertThat(ReportMath.collectedPercent(new BigDecimal("2999.99"), new BigDecimal("3000"))).isEqualByComparingTo("99.99");
    }

    @Test
    @DisplayName("Averages are rounded half-up to 2 decimals; money always has 2 decimals")
    void averageAndMoney() {
        assertThat(ReportMath.average(new BigDecimal("240"), 3)).isEqualByComparingTo("80.00");
        assertThat(ReportMath.average(new BigDecimal("200"), 3)).isEqualByComparingTo("66.67");
        assertThat(ReportMath.average(BigDecimal.ZERO, 0)).isNull();
        assertThat(ReportMath.money(new BigDecimal("1000")).toPlainString()).isEqualTo("1000.00");
        assertThat(ReportMath.money(null).toPlainString()).isEqualTo("0.00");
    }
}
