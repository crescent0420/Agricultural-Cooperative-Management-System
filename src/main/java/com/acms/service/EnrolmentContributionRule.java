package com.acms.service;

import com.acms.entity.Farmer;
import com.acms.entity.Season;

/**
 * "Only farmers who have paid at least 30 percent of the season's input contribution may enrol."
 * Enrolment calls this at its fixed place in the order of checks. The payment data arrives in Step 6,
 * which replaces the temporary implementation with the real one.
 */
public interface EnrolmentContributionRule {

    /** Throws BusinessRuleException (422) when the farmer has paid less than the required share. */
    void requireEnrolmentThreshold(Farmer farmer, Season season);
}
