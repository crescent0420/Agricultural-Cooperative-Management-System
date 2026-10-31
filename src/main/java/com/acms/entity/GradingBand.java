package com.acms.entity;

import com.acms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One line of the grading scale: a final score of at least minScore earns this grade. */
@Entity
@Table(name = "grading_bands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradingBand extends BaseEntity {

    @Column(nullable = false, length = 5)
    private String grade;

    @Column(name = "min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScore;

    @Column(length = 100)
    private String description;

    public GradingBand(String grade, BigDecimal minScore, String description) {
        this.grade = grade;
        this.minScore = minScore;
        this.description = description;
    }
}
