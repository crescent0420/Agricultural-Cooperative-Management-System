package com.acms.entity;

import com.acms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One of Rwanda's provinces (or Kigali City). Created by a migration; read-only in the API. */
@Entity
@Table(name = "provinces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Province extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /** Used by ReferenceDataSeeder for a province missing from the database. */
    public Province(String name) {
        this.name = name;
    }
}
