package com.acms.entity;

import com.acms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One of Rwanda's 30 districts. Seeded by a migration; the API can only correct a name. */
@Entity
@Table(name = "districts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class District extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /** Null only for districts typed in by hand before the official list was seeded. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    public District(String name) {
        this.name = name;
    }

    /** Used by ReferenceDataSeeder for an official district missing from the database. */
    public District(String name, Province province) {
        this.name = name;
        this.province = province;
    }

    /** Gives a district typed in by hand (no province yet) its official province. */
    public void assignProvince(Province province) {
        this.province = province;
    }

    public void setName(String name) {
        this.name = name;
    }
}
