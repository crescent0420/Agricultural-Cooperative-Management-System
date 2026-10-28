package com.acms.entity;

import com.acms.common.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "crop_programmes")
public class CropProgramme extends CreatedEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false, updatable = false)
    private District district;

    @ManyToMany
    @BatchSize(size = 50)
    @JoinTable(
            name = "programme_prerequisites",
            joinColumns = @JoinColumn(name = "programme_id"),
            inverseJoinColumns = @JoinColumn(name = "training_id"))
    private Set<Training> prerequisites = new HashSet<>();

    protected CropProgramme() {
    }

    public CropProgramme(String name, District district) {
        this.name = name;
        this.district = district;
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
}
