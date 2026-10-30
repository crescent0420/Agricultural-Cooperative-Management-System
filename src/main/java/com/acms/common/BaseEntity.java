package com.acms.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

/**
 * Root of EVERY entity: the database-generated id, when the row was created and when it last changed.
 * @MappedSuperclass = no table of its own; these three columns belong to each entity's own table.
 *
 * Both times are set here, automatically, so no entity or service sets them by hand:
 *  - before the first INSERT: created_at = updated_at = now;
 *  - before every UPDATE: updated_at = now (the database trigger set_updated_at does the same for changes
 *    made outside the application).
 * Records that never change (payments history, corrections, permits, the audit log...) keep
 * updated_at = created_at forever.
 *
 * equals/hashCode are deliberately NOT overridden: Java's default (same object) is always safe with
 * Hibernate, while id-based versions break for new entities (no id yet) and lazy proxies.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** True until the entity has been saved for the first time. */
    public boolean isNew() {
        return id == null;
    }

    /** Runs before the INSERT. (Subclasses must not reuse this method name, or it would be replaced.) */
    @PrePersist
    void stampTimesOnCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Runs before every UPDATE. */
    @PreUpdate
    void stampUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}
