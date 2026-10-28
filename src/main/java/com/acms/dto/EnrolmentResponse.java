package com.acms.dto;

import com.acms.entity.Enrolment;
import com.acms.entity.EnrolmentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record EnrolmentResponse(
        Long id,
        Long offeringId,
        Long programmeId,
        String programmeName,
        Long seasonId,
        String seasonName,
        Long farmerId,
        String membershipNumber,
        String farmerName,
        BigDecimal hectares,
        EnrolmentStatus status,
        Instant enrolledAt,
        Instant cancelledAt,
        Long cancelledById,
        String cancellationReason) {

    public static EnrolmentResponse from(Enrolment enrolment) {
        return new EnrolmentResponse(
                enrolment.getId(),
                enrolment.getOffering().getId(),
                enrolment.getOffering().getProgramme().getId(),
                enrolment.getOffering().getProgramme().getName(),
                enrolment.getOffering().getSeason().getId(),
                enrolment.getOffering().getSeason().getName(),
                enrolment.getFarmer().getId(),
                enrolment.getFarmer().getMembershipNumber(),
                enrolment.getFarmer().getFullName(),
                enrolment.getHectares(),
                enrolment.getStatus(),
                enrolment.getEnrolledAt(),
                enrolment.getCancelledAt(),
                enrolment.getCancelledBy() == null ? null : enrolment.getCancelledBy().getId(),
                enrolment.getCancellationReason());
    }
}
