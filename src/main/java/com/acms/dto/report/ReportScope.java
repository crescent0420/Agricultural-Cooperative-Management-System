package com.acms.dto.report;

/**
 * The filters a report was produced with. null = not filtered.
 * fieldOfficerId is set when a Field Officer asks: they only see the offerings assigned to them.
 */
public record ReportScope(Long districtId, Long seasonId, Long fieldOfficerId) {
}
