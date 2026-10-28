package com.acms.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

/**
 * An entity with created_at AND updated_at. updated_at is refreshed before every UPDATE
 * (the database trigger set_updated_at does the same for changes made outside the application).
 */
@MappedSuperclass
public abstract class AuditableEntity extends CreatedEntity {

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void stampUpdatedAtOnCreate() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void stampUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}
