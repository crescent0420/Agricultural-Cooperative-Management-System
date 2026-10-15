package com.acms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "crop_programmes")
public class CropProgramme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false, updatable = false)
    private District district;

    @ManyToMany
    @JoinTable(
            name = "programme_prerequisites",
            joinColumns = @JoinColumn(name = "programme_id"),
            inverseJoinColumns = @JoinColumn(name = "training_id"))
    private Set<Training> prerequisites = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CropProgramme() {
    }

    public CropProgramme(String name, District district) {
        this.name = name;
        this.district = district;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean hasPrerequisite(Long trainingId) {
        return prerequisites.stream()
                .anyMatch(training -> trainingId.equals(training.getId()));
    }

    public void addPrerequisite(Training training) {
        prerequisites.add(training);
    }

    public boolean removePrerequisite(Long trainingId) {
        return prerequisites.removeIf(
                training -> trainingId.equals(training.getId()));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public District getDistrict() {
        return district;
    }

    public Set<Training> getPrerequisites() {
        return Collections.unmodifiableSet(prerequisites);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
