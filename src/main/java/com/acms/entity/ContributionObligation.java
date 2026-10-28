package com.acms.entity;

import com.acms.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The input contribution one farmer owes for one season. What has been paid is NOT stored here:
 * it is always calculated from the confirmed payments, so it can never drift out of step with them.
 */
@Entity
@Table(name = "contribution_obligations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionObligation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false, updatable = false)
    private Farmer farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false, updatable = false)
    private Season season;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private AppUser createdBy;

    public ContributionObligation(Farmer farmer, Season season, BigDecimal amountDue, AppUser createdBy) {
        this.farmer = farmer;
        this.season = season;
        this.amountDue = amountDue;
        this.createdBy = createdBy;
    }

    /** Only ContributionService calls this, always together with a ContributionAdjustment record. */
    public void changeAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }
}
