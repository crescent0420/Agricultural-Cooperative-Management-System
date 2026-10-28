package com.acms.entity;

import com.acms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "farmer_trainings")
public class FarmerTraining extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false, updatable = false)
    private Farmer farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_id", nullable = false, updatable = false)
    private Training training;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", updatable = false)
    private AppUser recordedBy;

    @Column(name = "completed_on", nullable = false, updatable = false)
    private LocalDate completedOn;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected FarmerTraining() {
    }

    public FarmerTraining(
            Farmer farmer,
            Training training,
            LocalDate completedOn,
            AppUser recordedBy) {
        this.farmer = farmer;
        this.training = training;
        this.completedOn = completedOn;
        this.recordedBy = recordedBy;
    }

    @PrePersist
    void onCreate() {
        this.recordedAt = Instant.now();
    }

    public Farmer getFarmer() {
        return farmer;
    }

    public Training getTraining() {
        return training;
    }

    public AppUser getRecordedBy() {
        return recordedBy;
    }

    public LocalDate getCompletedOn() {
        return completedOn;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}