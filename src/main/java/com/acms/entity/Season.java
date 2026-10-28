package com.acms.entity;

import com.acms.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seasons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    /** BigDecimal, not double: hectares are added up and must not suffer rounding errors. */
    @Column(name = "max_land_hectares", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxLandHectares;

    @Column(name = "distribution_date", nullable = false)
    private LocalDate distributionDate;

    public Season(String name, LocalDate startsOn, LocalDate endsOn,
                  BigDecimal maxLandHectares, LocalDate distributionDate) {
        update(name, startsOn, endsOn, maxLandHectares, distributionDate);
    }

    public void update(String name, LocalDate startsOn, LocalDate endsOn,
                       BigDecimal maxLandHectares, LocalDate distributionDate) {
        this.name = name;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.maxLandHectares = maxLandHectares;
        this.distributionDate = distributionDate;
    }

    public boolean hasEnded(LocalDate today) {
        return today.isAfter(endsOn);
    }
}
