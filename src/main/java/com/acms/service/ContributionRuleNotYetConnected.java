package com.acms.service;

import com.acms.entity.Farmer;
import com.acms.entity.Season;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TEMPORARY (Step 5c): contributions and payments do not exist yet, so nothing can be checked.
 * Step 6 deletes this class and adds the real 30% rule. Until then every farmer passes this check.
 */
@Slf4j
@Component
public class ContributionRuleNotYetConnected implements EnrolmentContributionRule {

    @Override
    public void requireEnrolmentThreshold(Farmer farmer, Season season) {
        log.debug("30% contribution rule not connected yet (Step 6): farmer {} season {}",
                farmer.getId(), season.getId());
    }
}
