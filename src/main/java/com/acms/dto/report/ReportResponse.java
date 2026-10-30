package com.acms.dto.report;

import java.time.Instant;
import java.util.List;

/**
 * Every management report has the same shape:
 *   report      - which report this is
 *   basis       - in one or two sentences, what is counted and how (the same text for every district,
 *                 so two districts can never disagree about what a number means)
 *   scope       - the filters that were applied (district staff are always limited to their own district)
 *   generatedAt - the moment of the snapshot; all numbers in one report come from that single moment
 *   rows        - one line per cooperative / offering / season-and-district
 *   totals      - the sum of the rows, calculated from the same rows (never a separate query)
 */
public record ReportResponse<R, T>(
        String report,
        String basis,
        ReportScope scope,
        Instant generatedAt,
        List<R> rows,
        T totals) {
}
