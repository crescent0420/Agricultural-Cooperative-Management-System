package com.acms.dto;

import com.acms.entity.CollectionPermit;
import java.time.Instant;
import java.time.LocalDate;

public record CollectionPermitResponse(
        Long id,
        String permitNumber,
        Long enrolmentId,
        Long farmerId,
        String membershipNumber,
        String farmerName,
        String programmeName,
        String seasonName,
        LocalDate distributionDate,
        Instant issuedAt) {

    public static CollectionPermitResponse from(CollectionPermit permit) {
        var offering = permit.getEnrolment().getOffering();
        return new CollectionPermitResponse(
                permit.getId(),
                permit.getPermitNumber(),
                permit.getEnrolment().getId(),
                permit.getFarmer().getId(),
                permit.getFarmer().getMembershipNumber(),
                permit.getFarmer().getFullName(),
                offering.getProgramme().getName(),
                offering.getSeason().getName(),
                offering.getSeason().getDistributionDate(),
                permit.getIssuedAt());
    }
}
