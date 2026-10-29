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

/** One field inspection of an offering, e.g. "Pest and disease control": max 10 points, 25% of the final score. */
@Entity
@Table(name = "inspections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inspection extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false, updatable = false)
    private ProgrammeOffering offering;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private AppUser createdBy;

    public Inspection(ProgrammeOffering offering, String name, BigDecimal maxScore, BigDecimal weight,
                      AppUser createdBy) {
        this.offering = offering;
        this.createdBy = createdBy;
        update(name, maxScore, weight);
    }

    public void update(String name, BigDecimal maxScore, BigDecimal weight) {
        this.name = name;
        this.maxScore = maxScore;
        this.weight = weight;
    }
}
