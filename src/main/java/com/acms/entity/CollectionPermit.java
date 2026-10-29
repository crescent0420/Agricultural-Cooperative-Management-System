package com.acms.entity;

import com.acms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Permission to collect the inputs of ONE enrolment. Never changed or deleted (database trigger).
 * Whether it is still valid (enrolment active, farmer Active, fully paid, distribution not over)
 * is decided when the storekeeper verifies it, from the current data.
 */
@Entity
@Immutable
@Table(name = "collection_permits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionPermit extends BaseEntity {

    @Column(name = "permit_number", nullable = false, length = 20)
    private String permitNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrolment_id", nullable = false)
    private Enrolment enrolment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private AppUser issuedBy;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    public CollectionPermit(String permitNumber, Enrolment enrolment, Farmer farmer, AppUser issuedBy) {
        this.permitNumber = permitNumber;
        this.enrolment = enrolment;
        this.farmer = farmer;
        this.issuedBy = issuedBy;
        this.issuedAt = Instant.now();
    }
}
