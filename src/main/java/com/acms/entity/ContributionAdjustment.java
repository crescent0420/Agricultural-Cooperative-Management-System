package com.acms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** One change of an amount owed: old value, new value, reason, who and when. Never changed or deleted. */
@Entity
@Immutable
@Table(name = "contribution_adjustments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obligation_id", nullable = false)
    private ContributionObligation obligation;

    @Column(name = "old_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal oldAmount;

    @Column(name = "new_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal newAmount;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adjusted_by", nullable = false)
    private AppUser adjustedBy;

    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    public ContributionAdjustment(ContributionObligation obligation, BigDecimal oldAmount, BigDecimal newAmount,
                                  String reason, AppUser adjustedBy) {
        this.obligation = obligation;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = Instant.now();
    }
}
