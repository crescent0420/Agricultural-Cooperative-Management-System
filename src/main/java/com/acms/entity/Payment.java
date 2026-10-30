package com.acms.entity;

import com.acms.common.BaseEntity;
import com.acms.enums.PaymentMethod;
import com.acms.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A payment request. It counts towards the balance only once CONFIRMED.
 * The request details are updatable = false here AND protected by a database trigger.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obligation_id", nullable = false, updatable = false)
    private ContributionObligation obligation;

    @Column(name = "transaction_reference", nullable = false, updatable = false, length = 64)
    private String transactionReference;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PaymentStatus status;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false, updatable = false)
    private AppUser requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private AppUser confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failed_by")
    private AppUser failedBy;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public Payment(ContributionObligation obligation, String transactionReference, BigDecimal amount,
                   PaymentMethod method, AppUser requestedBy) {
        this.obligation = obligation;
        this.transactionReference = transactionReference;
        this.amount = amount;
        this.method = method;
        this.requestedBy = requestedBy;
        this.requestedAt = Instant.now();
        this.status = PaymentStatus.PENDING;
    }

    /** True when a repeated request carries exactly the same details as the original one. */
    public boolean isSameRequest(Long obligationId, BigDecimal amount, PaymentMethod method) {
        return obligation.getId().equals(obligationId)
                && this.amount.compareTo(amount) == 0
                && this.method == method;
    }

    public void confirm(AppUser confirmedBy, String providerReference) {
        this.status = PaymentStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = Instant.now();
        this.providerReference = providerReference;
    }

    public void fail(AppUser failedBy, String reason) {
        this.status = PaymentStatus.FAILED;
        this.failedBy = failedBy;
        this.failedAt = Instant.now();
        this.failureReason = reason;
    }
}
