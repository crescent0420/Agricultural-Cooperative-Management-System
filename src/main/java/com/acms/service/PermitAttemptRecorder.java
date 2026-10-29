package com.acms.service;

import com.acms.common.BaseService;
import com.acms.entity.AppUser;
import com.acms.entity.PermitAttempt;
import com.acms.entity.PermitOutcome;
import com.acms.repository.PermitAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records every permit attempt. A separate bean, so the transaction settings below really apply
 * (Spring's proxy is bypassed when a class calls its own methods).
 */
@Service
@RequiredArgsConstructor
public class PermitAttemptRecorder extends BaseService {

    private final PermitAttemptRepository attemptRepository;

    /**
     * REFUSED attempts: REQUIRES_NEW = in their own transaction, which commits even though the
     * permit request itself is rolled back. (The attempts table has no foreign keys, so this inner
     * transaction never waits for a row the outer one has locked.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRefusal(AppUser user, Long farmerId, Long enrolmentId, String reason) {
        attemptRepository.save(new PermitAttempt(enrolmentId, farmerId, user.getId(), user.getUsername(),
                PermitOutcome.REFUSED, reason, null));
    }

    /** ISSUED / ALREADY_ISSUED: MANDATORY = part of the same transaction as the permit itself. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSuccess(AppUser user, Long farmerId, Long enrolmentId, PermitOutcome outcome, Long permitId) {
        attemptRepository.save(new PermitAttempt(enrolmentId, farmerId, user.getId(), user.getUsername(),
                outcome, null, permitId));
    }
}
