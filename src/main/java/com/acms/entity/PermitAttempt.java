package com.acms.entity;

import com.acms.common.BaseEntity;
import com.acms.enums.PermitOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * One attempt to obtain a collection permit, successful or refused. Plain ids, no relations, like the
 * audit log: even an attempt with a non-existent or someone else's enrolment id is recorded.
 */
@Entity
@Immutable
@Table(name = "permit_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PermitAttempt extends BaseEntity {

    @Column(name = "enrolment_id")
    private Long enrolmentId;

    @Column(name = "farmer_id")
    private Long farmerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PermitOutcome outcome;

    @Column(length = 500)
    private String reason;

    @Column(name = "permit_id")
    private Long permitId;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    public PermitAttempt(Long enrolmentId, Long farmerId, Long userId, String username,
                         PermitOutcome outcome, String reason, Long permitId) {
        this.enrolmentId = enrolmentId;
        this.farmerId = farmerId;
        this.userId = userId;
        this.username = username;
        this.outcome = outcome;
        this.reason = reason == null || reason.length() <= 500 ? reason : reason.substring(0, 500);
        this.permitId = permitId;
        this.attemptedAt = Instant.now();
    }
}
