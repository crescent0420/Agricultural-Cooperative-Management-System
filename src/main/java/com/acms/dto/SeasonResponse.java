package com.acms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SeasonResponse(
        Long id,
        String name,
        LocalDate startsOn,
        LocalDate endsOn,
        BigDecimal maxLandHectares,
        LocalDate distributionDate,
        boolean enrolmentOpen,
        Long openWindowId,
        Instant createdAt,
        Instant updatedAt) {
}
