package com.acms.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;

/** An entity whose table has a created_at column, filled automatically on the first save. */
@MappedSuperclass
public abstract class CreatedEntity extends BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Runs before the INSERT. (Subclasses must not reuse this method name, or it would be replaced.) */
    @PrePersist
    void stampCreatedAt() {
        this.createdAt = Instant.now();
    }
}
