package com.acms.dto;

import com.acms.entity.InspectionResult;
import com.acms.enums.ResultStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ResultResponse(
        Long id,
        Long enrolmentId,
        Long offeringId,
        String programmeName,
        String seasonName,
        Long farmerId,
        String membershipNumber,
        String farmerName,
        ResultStatus status,
        List<ScoreLine> scores,
        BigDecimal finalScore,
        String grade,
        Instant submittedAt,
        Instant publishedAt,
        int correctionCount) {

    public static ResultResponse from(InspectionResult result, List<ScoreLine> scores, int correctionCount) {
        var enrolment = result.getEnrolment();
        var offering = enrolment.getOffering();
        return new ResultResponse(
                result.getId(),
                enrolment.getId(),
                offering.getId(),
                offering.getProgramme().getName(),
                offering.getSeason().getName(),
                enrolment.getFarmer().getId(),
                enrolment.getFarmer().getMembershipNumber(),
                enrolment.getFarmer().getFullName(),
                result.getStatus(),
                scores,
                result.getFinalScore(),
                result.getGrade(),
                result.getSubmittedAt(),
                result.getPublishedAt(),
                correctionCount);
    }
}
