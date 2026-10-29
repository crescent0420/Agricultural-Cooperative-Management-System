package com.acms.dto;

import java.util.List;

/**
 * What the storekeeper sees: valid = true means "hand over the inputs".
 * When it is false, problems says why (enrolment cancelled, farmer suspended, not fully paid...).
 */
public record PermitVerificationResponse(
        boolean valid,
        List<String> problems,
        CollectionPermitResponse permit) {
}
