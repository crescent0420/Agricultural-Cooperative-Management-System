package com.acms.entity;

import com.acms.common.BaseEntity;
import com.acms.enums.ResultStatus;
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
 * One farmer's result in one offering. Its stage decides who may do what:
 *   DRAFT      the Field Officer enters and changes scores
 *   SUBMITTED  waiting for the District Agronomist (approve, or return to DRAFT)
 *   PUBLISHED  visible to the farmer; changes only through an authorised correction
 */
@Entity
@Table(name = "inspection_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InspectionResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrolment_id", nullable = false, updatable = false)
    private Enrolment enrolment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultStatus status;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(length = 5)
    private String grade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private AppUser submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private AppUser publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    public InspectionResult(Enrolment enrolment, AppUser createdBy) {
        this.enrolment = enrolment;
        this.createdBy = createdBy;
        this.status = ResultStatus.DRAFT;
    }

    public void submit(AppUser fieldOfficer, BigDecimal finalScore, String grade) {
        this.status = ResultStatus.SUBMITTED;
        this.submittedBy = fieldOfficer;
        this.submittedAt = Instant.now();
        this.finalScore = finalScore;
        this.grade = grade;
    }

    /** Sent back by the District Agronomist: editable again, so the calculated values are cleared. */
    public void returnToDraft() {
        this.status = ResultStatus.DRAFT;
        this.submittedBy = null;
        this.submittedAt = null;
        this.finalScore = null;
        this.grade = null;
    }

    public void publish(AppUser agronomist, BigDecimal finalScore, String grade) {
        this.status = ResultStatus.PUBLISHED;
        this.publishedBy = agronomist;
        this.publishedAt = Instant.now();
        this.finalScore = finalScore;
        this.grade = grade;
    }

    /** Only together with a new ResultCorrection row (the database checks this). */
    public void applyCorrection(BigDecimal finalScore, String grade) {
        this.finalScore = finalScore;
        this.grade = grade;
    }
}
