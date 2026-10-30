package com.acms.dto.report;

/** The sum of all the rows of the farmers-per-cooperative report. */
public record FarmerCountTotals(
        long cooperatives,
        long active,
        long suspended,
        long withdrawn,
        long retired,
        long total) {
}
