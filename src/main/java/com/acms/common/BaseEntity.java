package com.acms.common;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Root of every entity: the database-generated id.
 * @MappedSuperclass = no table of its own; its columns belong to each subclass's table.
 *
 *   BaseEntity          id
 *     CreatedEntity       + created_at
 *       AuditableEntity     + updated_at
 *
 * equals/hashCode are deliberately NOT overridden: Java's default (same object) is always safe with
 * Hibernate, while id-based versions break for new entities (no id yet) and lazy proxies.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    /** True until the entity has been saved for the first time. */
    public boolean isNew() {
        return id == null;
    }
}
