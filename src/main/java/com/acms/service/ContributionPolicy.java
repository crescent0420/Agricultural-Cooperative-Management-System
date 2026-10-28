package com.acms.service;

import com.acms.entity.ContributionObligation;
import com.acms.entity.Farmer;
import com.acms.entity.Season;
import com.acms.exception.BusinessRuleException;
import com.acms.repository.ContributionObligationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The rules that link payment to agricultural activity:
 *  - at least 30% of the season's contribution paid (confirmed)  -> may enrol
 *  - 100% paid                                                   -> may collect inputs (collection permits, Step 8)
 * Only CONFIRMED payments count. A farmer with no contribution recorded for the season may not enrol:
 * the Union cannot know whether they paid (assumption, stated in the report).
 */
@Component
@RequiredArgsConstructor
public class ContributionPolicy implements EnrolmentContributionRule {

    public static final int ENROLMENT_MIN_PERCENT = 30;
    public static final int COLLECTION_MIN_PERCENT = 100;

    private final ContributionService contributionService;
    private final ContributionObligationRepository obligationRepository;

    @Override
    public void requireEnrolmentThreshold(Farmer farmer, Season season) {
        requirePaidAtLeast(farmer, season, ENROLMENT_MIN_PERCENT, "enrol");
    }

    /** For collection permits (Step 8). */
    public void requireFullyPaid(Farmer farmer, Season season) {
        requirePaidAtLeast(farmer, season, COLLECTION_MIN_PERCENT, "collect inputs");
    }

    private void requirePaidAtLeast(Farmer farmer, Season season, int percent, String action) {
        Long obligationId = obligationRepository.findIdByFarmerAndSeason(farmer.getId(), season.getId())
                .orElseThrow(() -> new BusinessRuleException("No input contribution is recorded for this farmer in season '"
                        + season.getName() + "'; at least " + percent + "% must be paid to " + action + "."));
        // FOR SHARE first, then read: a confirmation or adjustment of this contribution cannot change
        // the numbers while we are deciding, and we decide on the latest committed numbers.
        obligationRepository.lockForShare(obligationId);
        ContributionObligation obligation = obligationRepository.findById(obligationId)
                .orElseThrow(() -> new BusinessRuleException("The contribution was not found."));
        ContributionBalance balance = contributionService.balanceOf(obligation);
        if (!balance.meets(percent)) {
            throw new BusinessRuleException("The farmer has paid " + balance.paidPercent() + "% of the "
                    + obligation.getAmountDue() + " contribution for season '" + season.getName()
                    + "'; at least " + percent + "% is required to " + action + ".");
        }
    }
}
