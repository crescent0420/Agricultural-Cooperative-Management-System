package com.acms.dto;

import com.acms.entity.ProgrammeOffering;
import java.math.BigDecimal;
import java.time.Instant;

public record ProgrammeOfferingResponse(
        Long id,
        Long programmeId,
        String programmeName,
        Long districtId,
        String districtName,
        Long seasonId,
        String seasonName,
        Long fieldOfficerId,
        String fieldOfficerUsername,
        BigDecimal hectaresPerParticipant,
        int maxParticipants,
        int enrolledCount,
        int remainingPlaces,
        Instant createdAt,
        Instant updatedAt) {

    public static ProgrammeOfferingResponse from(ProgrammeOffering offering) {
        return new ProgrammeOfferingResponse(
                offering.getId(),
                offering.getProgramme().getId(),
                offering.getProgramme().getName(),
                offering.getProgramme().getDistrict().getId(),
                offering.getProgramme().getDistrict().getName(),
                offering.getSeason().getId(),
                offering.getSeason().getName(),
                offering.getFieldOfficer().getId(),
                offering.getFieldOfficer().getUsername(),
                offering.getHectaresPerParticipant(),
                offering.getMaxParticipants(),
                offering.getEnrolledCount(),
                offering.remainingPlaces(),
                offering.getCreatedAt(),
                offering.getUpdatedAt());
    }
}
