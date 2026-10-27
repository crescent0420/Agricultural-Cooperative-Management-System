package com.acms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A crop programme offered in one season: Maize (Huye) in Season 2026A, run by one Field Officer. */
@Entity
@Table(name = "programme_offerings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgrammeOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // updatable = false: a different programme or season would be a different offering.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programme_id", nullable = false, updatable = false)
    private CropProgramme programme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false, updatable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_officer_id", nullable = false)
    private AppUser fieldOfficer;

    @Column(name = "hectares_per_participant", nullable = false, precision = 6, scale = 2)
    private BigDecimal hectaresPerParticipant;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProgrammeOffering(CropProgramme programme, Season season, AppUser fieldOfficer,
                             BigDecimal hectaresPerParticipant, int maxParticipants) {
        this.programme = programme;
        this.season = season;
        this.fieldOfficer = fieldOfficer;
        this.hectaresPerParticipant = hectaresPerParticipant;
        this.maxParticipants = maxParticipants;
        this.enrolledCount = 0;
    }

    public int remainingPlaces() {
        return maxParticipants - enrolledCount;
    }

    public boolean hasEnrolments() {
        return enrolledCount > 0;
    }

    public void assignFieldOfficer(AppUser fieldOfficer) {
        this.fieldOfficer = fieldOfficer;
    }

    public void changeHectaresPerParticipant(BigDecimal hectaresPerParticipant) {
        this.hectaresPerParticipant = hectaresPerParticipant;
    }

    public void changeMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
