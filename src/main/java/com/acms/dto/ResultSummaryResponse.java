package com.acms.dto;

import com.acms.entity.InspectionResult;
import com.acms.enums.ResultStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ResultSummaryResponse(
        Long id,
        Long enrolmentId,
        Long offeringId,
        String programmeName,
        String seasonName,
        Long farmerId,
        String membershipNumber,
        String farmerName,
        ResultStatus status,
        BigDecimal finalScore,
        String grade,
        Instant publishedAt) {

    public static ResultSummaryResponse from(InspectionResult result) {
        var enrolment = result.getEnrolment();
        var offering = enrolment.getOffering();
        return new ResultSummaryResponse(
                result.getId(),
                enrolment.getId(),
                offering.getId(),
                offering.getProgramme().getName(),
                offering.getSeason().getName(),
                enrolment.getFarmer().getId(),
                enrolment.getFarmer().getMembershipNumber(),
                enrolment.getFarmer().getFullName(),
                result.getStatus(),
                result.getFinalScore(),
                result.getGrade(),
                result.getPublishedAt());
    }
}
