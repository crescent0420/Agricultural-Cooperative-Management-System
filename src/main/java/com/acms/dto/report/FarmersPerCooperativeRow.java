package com.acms.dto.report;

/** One cooperative and how many of its farmers are in each status. */
public record FarmersPerCooperativeRow(
        Long cooperativeId,
        String cooperativeName,
        Long districtId,
        String districtName,
        long active,
        long suspended,
        long withdrawn,
        long retired,
        long total) {
}
