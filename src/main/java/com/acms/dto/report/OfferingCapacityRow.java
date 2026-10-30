package com.acms.dto.report;

import java.math.BigDecimal;

/** One programme offering: how many places it has and how many are taken. */
public record OfferingCapacityRow(
        Long offeringId,
        Long programmeId,
        String programmeName,
        Long seasonId,
        String seasonName,
        Long districtId,
        String districtName,
        Long fieldOfficerId,
        String fieldOfficerUsername,
        int maxParticipants,
        long enrolled,
        long cancelled,
        long placesLeft,
        BigDecimal capacityUsedPercent,
        boolean full,
        BigDecimal hectaresPerParticipant,
        BigDecimal hectaresEnrolled) {
}
