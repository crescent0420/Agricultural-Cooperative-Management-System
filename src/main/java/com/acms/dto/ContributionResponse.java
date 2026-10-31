package com.acms.dto;

import com.acms.entity.ContributionObligation;
import com.acms.service.ContributionBalance;
import com.acms.service.ContributionPolicy;
import java.math.BigDecimal;
import java.time.Instant;

/** A contribution with its balance, calculated from the payments at the moment of the request. */
public record ContributionResponse(
        Long id,
        Long farmerId,
        String membershipNumber,
        String farmerName,
        Long seasonId,
        String seasonName,
        BigDecimal amountDue,
        BigDecimal confirmedPaid,
        BigDecimal pendingAmount,
        BigDecimal outstanding,
        BigDecimal paidPercent,
        boolean enrolmentAllowed,
        boolean inputCollectionAllowed,
        Instant createdAt,
        Instant updatedAt) {

    public static ContributionResponse from(ContributionObligation obligation, ContributionBalance balance) {
        return new ContributionResponse(
                obligation.getId(),
                obligation.getFarmer().getId(),
                obligation.getFarmer().getMembershipNumber(),
                obligation.getFarmer().getFullName(),
                obligation.getSeason().getId(),
                obligation.getSeason().getName(),
                balance.amountDue(),
                balance.confirmedPaid(),
                balance.pending(),
                balance.outstanding(),
                balance.paidPercent(),
                balance.meets(ContributionPolicy.ENROLMENT_MIN_PERCENT),
                balance.meets(ContributionPolicy.COLLECTION_MIN_PERCENT),
                obligation.getCreatedAt(),
                obligation.getUpdatedAt());
    }
}
