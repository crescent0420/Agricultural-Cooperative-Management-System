package com.acms.entity;

import com.acms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The score entered for one inspection of one result. After the DRAFT stage this is the ORIGINAL value. */
@Entity
@Table(name = "inspection_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InspectionScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false, updatable = false)
    private InspectionResult result;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false, updatable = false)
    private Inspection inspection;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entered_by", nullable = false)
    private AppUser enteredBy;

    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;

    public InspectionScore(InspectionResult result, Inspection inspection, BigDecimal score, AppUser enteredBy) {
        this.result = result;
        this.inspection = inspection;
        change(score, enteredBy);
    }

    public void change(BigDecimal score, AppUser enteredBy) {
        this.score = score;
        this.enteredBy = enteredBy;
        this.enteredAt = Instant.now();
    }
}
