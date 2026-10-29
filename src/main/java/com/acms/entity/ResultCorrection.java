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
import org.hibernate.annotations.Immutable;

/**
 * An authorised correction of a published result: the original and corrected value (of the score, the
 * final score and the grade), the reason, the person responsible and the time. Never changed or deleted.
 */
@Entity
@Immutable
@Table(name = "result_corrections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResultCorrection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private InspectionResult result;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "original_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal originalScore;

    @Column(name = "corrected_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal correctedScore;

    @Column(name = "original_final_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal originalFinalScore;

    @Column(name = "corrected_final_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal correctedFinalScore;

    @Column(name = "original_grade", nullable = false, length = 5)
    private String originalGrade;

    @Column(name = "corrected_grade", nullable = false, length = 5)
    private String correctedGrade;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corrected_by", nullable = false)
    private AppUser correctedBy;

    @Column(name = "corrected_at", nullable = false)
    private Instant correctedAt;

    public ResultCorrection(InspectionResult result, Inspection inspection,
                            BigDecimal originalScore, BigDecimal correctedScore,
                            BigDecimal originalFinalScore, BigDecimal correctedFinalScore,
                            String originalGrade, String correctedGrade,
                            String reason, AppUser correctedBy) {
        this.result = result;
        this.inspection = inspection;
        this.originalScore = originalScore;
        this.correctedScore = correctedScore;
        this.originalFinalScore = originalFinalScore;
        this.correctedFinalScore = correctedFinalScore;
        this.originalGrade = originalGrade;
        this.correctedGrade = correctedGrade;
        this.reason = reason;
        this.correctedBy = correctedBy;
        this.correctedAt = Instant.now();
    }
}
