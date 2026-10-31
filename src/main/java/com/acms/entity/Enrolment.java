package com.acms.entity;

import com.acms.common.BaseEntity;
import com.acms.enums.EnrolmentStatus;
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

/** One farmer in one programme offering. Cancelling changes the status; the row is never deleted. */
@Entity
@Table(name = "enrolments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrolment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false, updatable = false)
    private ProgrammeOffering offering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false, updatable = false)
    private Farmer farmer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EnrolmentStatus status;

    @Column(nullable = false, updatable = false, precision = 6, scale = 2)
    private BigDecimal hectares;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrolled_by", nullable = false, updatable = false)
    private AppUser enrolledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private AppUser cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    public Enrolment(ProgrammeOffering offering, Farmer farmer, BigDecimal hectares, AppUser enrolledBy) {
        this.offering = offering;
        this.farmer = farmer;
        this.hectares = hectares;
        this.enrolledBy = enrolledBy;
        this.status = EnrolmentStatus.ENROLLED;
        this.enrolledAt = Instant.now();
    }

    public boolean isActive() {
        return status == EnrolmentStatus.ENROLLED;
    }

    public void cancel(AppUser cancelledBy, String reason) {
        this.status = EnrolmentStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
    }
}
