package com.acms.dto;

import com.acms.entity.PermitAttempt;
import com.acms.entity.PermitOutcome;
import java.time.Instant;

public record PermitAttemptResponse(
        Long id,
        Long enrolmentId,
        Long farmerId,
        Long userId,
        String username,
        PermitOutcome outcome,
        String reason,
        Long permitId,
        Instant attemptedAt) {

    public static PermitAttemptResponse from(PermitAttempt a) {
        return new PermitAttemptResponse(a.getId(), a.getEnrolmentId(), a.getFarmerId(), a.getUserId(),
                a.getUsername(), a.getOutcome(), a.getReason(), a.getPermitId(), a.getAttemptedAt());
    }
}
