package com.acms.dto.report;

import java.math.BigDecimal;

/** The sum of all the rows of the enrolment-and-capacity report. */
public record OfferingCapacityTotals(
        long offerings,
        long fullOfferings,
        long maxParticipants,
        long enrolled,
        long cancelled,
        long placesLeft,
        BigDecimal capacityUsedPercent,
        BigDecimal hectaresEnrolled) {
}
